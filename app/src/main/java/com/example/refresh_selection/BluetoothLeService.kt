package com.example.refresh_selection

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter.EXTRA_DATA
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.util.*


private val TAG = BluetoothLeService::class.java.simpleName
private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
const val ACTIVITY_DATA_FETCH = "ACTIVITY_DATA_FETCH"
val BASE_SERVICE_UUID: UUID = UUID.fromString("0000FEE0-0000-1000-8000-00805f9b34fb")
val CONTROL_POINT_UUID: UUID = UUID.fromString("00000004-0000-3512-2118-0009af100700")
val ACTIVITY_UUID: UUID = UUID.fromString("00000005-0000-3512-2118-0009af100700")
val REAL_TIME_STEP_UUID: UUID = UUID.fromString("00000007-0000-3512-2118-0009af100700")
val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


// A service that interacts with the BLE device via the Android BLE API.
class BluetoothLeService() : Service() {
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED
    private var i = 0;

    private var connectionState = STATE_DISCONNECTED

    /**
     * 블루투스 디바이스 GATT 서버 연결
     */
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

    private val mGattCallback = object : BluetoothGattCallback() {
        /**
         *
         */
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
         * 서비스 발견시 실행되는 콜백 함수
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val intentAction: String
            intentAction = ACTION_GATT_SERVICES_DISCOVERED
            broadcastUpdate(intentAction)
        }

        /**
         * write Descriptor
         * 특성의 설명자 작성시 실행
         */
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            setFetchValue()
        }

        /**
         * Read Characteristic
         * 특성의 값을 읽으면 실행
         */
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
         */
        override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {
            when( characteristic.uuid ) {
                //fetch status
                CONTROL_POINT_UUID -> {
                    var value = characteristic.value
                    if(value[0]==16.toByte() && value[1]==1.toByte() && value[2]==1.toByte()) {
                        characteristic.value = byteArrayOf(2.toByte())
                        mBluetoothGatt!!.writeCharacteristic(characteristic)
                    } else if(value[0]==16.toByte() && value[1]==2.toByte() && value[2]==1.toByte()) {
                        characteristic.value = byteArrayOf(3.toByte())
                        mBluetoothGatt!!.writeCharacteristic(characteristic)
                    }else if(value[0]==16.toByte() && value[1]==3.toByte() && value[2]==1.toByte()) {

                    }
                }
                //activity data 도착
                ACTIVITY_UUID -> {
                    broadcastUpdate(ACTIVITY_DATA_FETCH, characteristic)
                }
            }
        }
    }

    /**
     * 특성에 대한 알림을 on/off
     */
    fun setCharacteristicNotification(characteristicUUID: UUID, enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        val characteristic = mBluetoothGatt!!.getService(BASE_SERVICE_UUID).getCharacteristic(characteristicUUID);
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        if( enabled ) {
            val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID).apply {
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            mBluetoothGatt!!.writeDescriptor(descriptor)
        } else {
            val descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID).apply {
                value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }

    fun setFetchValue() {
        var characteristic = mBluetoothGatt!!.getService(BASE_SERVICE_UUID).getCharacteristic(CONTROL_POINT_UUID);
        var data = byteArrayOf(1.toByte(),1.toByte(), 229.toUByte().toByte(),7.toByte(),4.toByte(),30.toByte(),12.toByte(), 0.toByte(), 0.toByte(), 24.toByte());
        characteristic.value = data;
        mBluetoothGatt!!.writeCharacteristic(characteristic);
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        when (characteristic.uuid) {
            REAL_TIME_STEP_UUID -> {
                val data = characteristic.value;
                val totalSteps = (data[1].toInt().and(255)) or (((data[2].toInt().and(255)).shl(8)))
                val distance = (data[5].toInt().and(255)) or (data[6].toInt().and(255)) or (data[7].toInt().and(16711680)) or (data[8].toInt().and(255)).shl(8)
                intent.putExtra("totalSteps", totalSteps)
                intent.putExtra("distance", distance)
            }
            //activity data 도착
            ACTIVITY_UUID -> {
                var data = characteristic.value;
                data.forEachIndexed{
                    index,value ->
                    if(index==3 || index==7 || index==11 || index==15 ) {
                        if(value.toInt()!=0) {
                            Log.d("steps",value.toString())
                        }
                        Log.d("time", i++.toString())
                    }
                }
            }
        }
        sendBroadcast(intent)
    }


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
        get() {
            if (mBluetoothGatt == null) return null

            return mBluetoothGatt!!.services
        }

}