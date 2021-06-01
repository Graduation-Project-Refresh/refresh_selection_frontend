package com.example.refresh_selection

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


private val TAG = BluetoothLeService::class.java.simpleName
private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
const val WRITE_DESCRIPTOR = "WRITE_DESCRIPTOR"
const val ACTIVITY_DATA_FETCH = "ACTIVITY_DATA_FETCH"
const val ACTIVITY_DATA_SEND_OVER="ACTIVITY_DATA_SEND_OVER"

val notifications_characteristic:UUID=UUID.fromString("00000010-0000-3512-2118-0009af100700")//? 지우던가..
val BASE_SERVICE_UUID: UUID = UUID.fromString("0000FEE0-0000-1000-8000-00805f9b34fb")
val CONTROL_POINT_UUID: UUID = UUID.fromString("00000004-0000-3512-2118-0009af100700")//fetch관련 흠...? char_fetch
val ACTIVITY_UUID: UUID = UUID.fromString("00000005-0000-3512-2118-0009af100700")//activiti_data
val REAL_TIME_STEP_UUID: UUID = UUID.fromString("00000007-0000-3512-2118-0009af100700")
val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val descriptorWriteQueue: Queue<BluetoothGattDescriptor> = LinkedList()

// A service that interacts with the BLE device via the Android BLE API.
class BluetoothLeService() : Service() {
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED
    private var connectionState = STATE_DISCONNECTED
    private var i = 0
    var sum_step=0//for문 밖으로 빼내용
    /**
     * 블루투스 디바이스 GATT 서버 연결
     */
    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }

        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress
            && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }

        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)

        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    private val mGattCallback = @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    object : BluetoothGattCallback() {
        /**
         * gatt 서버 연결 상태 변화시 실행
         */
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    Log.i(TAG, "Connected to GATT server.")
                    Log.i(TAG, "Attempting to start service discovery: " +
                            mBluetoothGatt?.discoverServices())
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                    broadcastUpdate(intentAction)
                }
            }
        }

        /**
         * discover services
         * 서비스 발견시 실행
         */
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val intentAction: String
            intentAction = ACTION_GATT_SERVICES_DISCOVERED
            broadcastUpdate(intentAction)
            setNotificationOn()
        }

        /**
         * write Descriptor
         * 특성의 설명자 작성시 실행
         */
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            descriptorWriteQueue.remove();
            if(descriptorWriteQueue.size > 0) {
                mBluetoothGatt!!.writeDescriptor(descriptorWriteQueue.element());
            } else {
                if (descriptor != null) {
                    if(descriptor.value == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
                        setFetchValue()
                    }
                }
            }
        }

        /**
         * Read Characteristic
         * 특성의 값을 읽으면 실행
         */
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
            }
        }

        /**
         * characteristic notification
         * 특성에 대한 알림 도착시 실행
         * 특성의 값이 변화하면 알림이 도착함
         */
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when( characteristic.uuid ) {
                //과거 데이터 fetch를 위한 Notify
                CONTROL_POINT_UUID -> {
                    var value = characteristic.value
                    if(value[0]==16.toByte() && value[1]==1.toByte() && value[2]==1.toByte()) {
                        characteristic.value = byteArrayOf(2.toByte())//0x02
                        mBluetoothGatt!!.writeCharacteristic(characteristic)
                    } else if(value[0]==16.toByte() && value[1]==2.toByte() && value[2]==1.toByte()) {
                        characteristic.value = byteArrayOf(3.toByte())//0x03
                        mBluetoothGatt!!.writeCharacteristic(characteristic)
                    }else if(value[0]==16.toByte() && value[1]==3.toByte() && value[2]==1.toByte()) {
                        setNotificationOff()//데이터 받는게 끝났을 때 보내는것
                        broadcastUpdate(ACTIVITY_DATA_SEND_OVER,characteristic )
                    }
                }
                //과거 활동 데이터 도착
                ACTIVITY_UUID -> {
                    broadcastUpdate(ACTIVITY_DATA_FETCH, characteristic)
                }
                //실시간 걸음 수 데이터 도착
                REAL_TIME_STEP_UUID -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }


            }
        }
    }



    /**
     * 특성에 대한 알림을 on/off
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setCharacteristicNotification(characteristicUUID: UUID, enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        val characteristic = mBluetoothGatt!!.getService(BASE_SERVICE_UUID).getCharacteristic(characteristicUUID);
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        val descriptor:  BluetoothGattDescriptor
        if( enabled ) {
            descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID).apply {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
        } else {
            descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID).apply {
                value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
        }
        descriptorWriteQueue.add(descriptor)
        if ( descriptorWriteQueue.size == 1 ) {
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }

    /**
     * MainActivity로 브로드캐스트를 날리기 위한 메소드
     */
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    /**
     * MainActivity로 브로드캐스트를 날리기 위한 메소드
     */

    @RequiresApi(Build.VERSION_CODES.O)
    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        when (characteristic.uuid) {
            REAL_TIME_STEP_UUID -> {
                val data = characteristic.value;
                val totalSteps = (data[1].toInt().and(255)) or (((data[2].toInt().and(255)).shl(8)))
                val distance = (data[5].toInt().and(255)) or (data[6].toInt().and(255)) or (data[7].toInt().and(16711680)) or (data[8].toInt().and(255)).shl(8)
                Log.d("totalSteps",totalSteps.toString())
                Log.d("distance",distance.toString())
                intent.putExtra("totalSteps", totalSteps)
                intent.putExtra("distance", distance)
            }
            //activity data 도착 //과거 활동 데이터 도착?
            ACTIVITY_UUID -> {
                var data = characteristic.value;

                var nowdate = LocalDateTime.now()
                nowdate=nowdate.minusDays(7)

                val dateTime: LocalDateTime =nowdate
                    // LocalDateTime.of(2021, 5, 23, 15, 0,0)
                //가져오기 시작할 날짜

                data.forEachIndexed{
                    //1분단위로 저장되어잇는걸 다 보냄
                    //일주일 단위로 추천
                    index,value ->
                    if(index==3 || index==7 || index==11 || index==15 ) {

                        i++
                        if(value.toInt()!=0) {//걸음수가 0인건 안찍혀요

                            sum_step=sum_step+value.toInt()
                            Log.d("sum_step",sum_step.toString())
                            Log.d("steps",value.toString())
                            Log.d("time", dateTime.plusMinutes(i.toLong()).toString())
//                            formatted
//                        dateTime.plusMinutes(i.toLong()).toString()
                            // 가져올수 있는 개수가 한정되어있을거다...
                        }
                    }
                }

            }
            CONTROL_POINT_UUID->{
                Log.d("recommend_step", sum_step.toString())
                intent.putExtra("recommend_step", (sum_step/7.toInt()/100)*100)
            }

        }

        sendBroadcast(intent)
    }

    /**
     * 과거 데이터 fetch 시 필요한 notification on
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setNotificationOn() {
        setCharacteristicNotification(ACTIVITY_UUID,true)
        setCharacteristicNotification(REAL_TIME_STEP_UUID,true)
        setCharacteristicNotification(CONTROL_POINT_UUID,true)
    }

    /**
     * 과거 데이터를 다 받고 notification off 할 때 사용
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setNotificationOff() {
        setCharacteristicNotification(ACTIVITY_UUID,false)
        setCharacteristicNotification(CONTROL_POINT_UUID,false)
    }

    /**
     * 과거 데이터 fetch 시 필요
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setFetchValue() {
        var characteristic = mBluetoothGatt!!.getService(BASE_SERVICE_UUID).getCharacteristic(CONTROL_POINT_UUID);//gatt 서버랑 연결후 마지막으로 동기화할 날짜를 바이트로 변환후 보낸다
        //가져올 시작 날짜를 바이트로 넣으면 현재까지의 데이터를 읽어옴
        var nowdate = LocalDate.now()
        nowdate=nowdate.minusDays(7)


        var time=LocalTime.now()


        var year=nowdate.year.toInt()
        var data= byteArrayOf(1.toByte(),1.toByte(),(year/100).toByte(),(year%100).toByte() ,nowdate.monthValue.toByte(),nowdate.dayOfMonth.toByte(),time.hour.toByte(),time.minute.toByte() ,time.second.toByte(),24.toByte())
        //var data = byteArrayOf(1.toByte(),1.toByte(), 229.toUByte().toByte(),7.toByte(),5.toByte(),23.toByte(),15.toByte(), 0.toByte(), 0.toByte(), 24.toByte());
        characteristic.value = data;
        mBluetoothGatt!!.writeCharacteristic(characteristic);
    }


    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    private val mBinder: IBinder = LocalBinder()

    val supportedGattServices: List<BluetoothGattService>?
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        get() {
            if (mBluetoothGatt == null) return null

            return mBluetoothGatt!!.services
        }

}