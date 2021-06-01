package com.example.refresh_selection

import android.Manifest
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*


private const val SCAN_PERIOD: Long = 10000
private val TAG = BluetoothLeService::class.java.simpleName

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MainActivity : AppCompatActivity(){
    private val REQUEST_ENABLE_BT = 3
    private var leDeviceListAdapter: LeDeviceListAdapter? = null
    private var mScanning: Boolean = false
    private var handler: Handler? = null
    private var mibandDevice: BluetoothDevice? = null;
    var bluetoothLeService : BluetoothLeService? = null
    private var mConnected = false
    private var isBindedService = false
    private var setServiceNotification: List<UUID>? = null;

    ////은바니
    private val COL_DATE: Int = 0
    private val COL_TIME = 1
    private val COL_STEPS = 2
////
    private fun PackageManager.missingSystemFeature(name: String): Boolean = !hasSystemFeature(name)
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private var mBtn: Button? = null
    private var scanBtn: Button? = null
    private var showBt:Button?=null

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(mibandDevice?.address)
            Log.d(TAG, "Connect request result=" + result)
        }
    }

    /**
     * 앱 시작 시 실행
     */
    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getBluetoothPermissions()

        //변수 초기화
        handler = Handler()
        mBtn = findViewById<Button>(R.id.pairBt)
        scanBtn = findViewById<Button>(R.id.scan)
        showBt=findViewById<Button>(R.id.showBt)
        leDeviceListAdapter = LeDeviceListAdapter()

        packageManager.takeIf { it.missingSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
//            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        //페어링된 디바이스가 있는지 확인
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
        }
        if (pairedDevices != null) {
            for (device in pairedDevices) {
                if (device.name.contains("Mi band")) {
                    mibandDevice = device;
                    setMibandDeviceInfoView()
                    break
                }
            }
        }

        //페어링된 디바이스가 없으면 블루투스 스캔 시작
        if(mibandDevice == null){
            scanLeDevice(true)
        }

        //paring 버튼 이벤트. 페어링을 시작함.
        mBtn!!.setOnClickListener {
//            mBtn?.visibility = View.GONE
//            findViewById<View>(R.id.scrollView).visibility = View.GONE
            mibandDevice?.name?.let { it1 -> Log.d("mibandDevice", it1) }
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            isBindedService = bindService(
                gattServiceIntent,
                mServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }

        //scan button event. 디바이스를 못 찾을 경우 나타나는 버튼으로 클릭 시 다시 스캔을 시작
        scanBtn!!.setOnClickListener {
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            scanBtn?.visibility = View.INVISIBLE
            scanLeDevice(true)
        }

        showBt!!.setOnClickListener {
            //설문조사가 잘 저장됨
            val nextIntent = Intent(this, ModelClient::class.java)
            startActivity(nextIntent)
            //설문조사 저장후 메인으로 넘어감

        }
//        //추천 걸음수
//        try {
//            val manager = assets
//            val `in`: InputStream = manager.open("test.csv")
//            val cooked: ArrayList<Data> = parse(`in`)!!
//            var all_steps = ""
//            if (cooked != null) {
//                for (piece in cooked) {
//                    all_steps += piece.steps.toString() + " "
//                }
//            }
//            println(all_steps)
//            val all_steps_strArr = all_steps.split(" ").toTypedArray()
//            val all_steps_intArr =
//                Arrays.stream(all_steps_strArr).mapToInt(Integer::parseInt).toArray()
//            println(all_steps_intArr)
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }

    }

//    //은바니 csv 읽는 메소드
//    @Throws(IOException::class)
//    private fun parse(`in`: InputStream): ArrayList<Data>? {
//        val results = ArrayList<Data>()
//        val reader = BufferedReader(InputStreamReader(`in`))
//        var nextLine: String = reader.readLine()
//        while (reader.readLine().also({ nextLine = it }) != null) {
//            val tokens = nextLine.split(",").toTypedArray()
//            if (tokens.size != 3) {
//                Log.w("CSVParser", "Skipping Bad CSV Row")
//                continue
//            }
//            //Add new parsed result
//            val current = Data()
//            current.date = tokens[COL_DATE]
//            current.time = tokens[COL_TIME]
//            current.steps = tokens[COL_STEPS]
//            results.add(current)
//        }
//        `in`.close()
//        return results
//    }
    /**
     * 미밴드 디바이스 정보를 뷰에 나타냄
     */
    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    fun setMibandDeviceInfoView() {
        val textView: TextView = findViewById<TextView>(R.id.deviceName)
        val textView2: TextView = findViewById<TextView>(R.id.address)
        findViewById<View>(R.id.scrollView).visibility = View.VISIBLE
        scanBtn?.visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        mBtn?.visibility = View.VISIBLE
//        textView.append(": "+mibandDevice!!.name)
//        textView2.append(": "+mibandDevice!!.address)
        textView.text = mibandDevice!!.name
        textView2.text = mibandDevice!!.address
    }

    /**
     * 앱의 권한을 설정
     */
    fun getBluetoothPermissions() {
        val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        val permission3 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val permission4 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission1 != PackageManager.PERMISSION_GRANTED
                || permission2 != PackageManager.PERMISSION_GRANTED
                || permission3 != PackageManager.PERMISSION_GRANTED
                || permission4 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                642
            )
        } else {
            Log.d("DISCOVERING-PERMISSIONS", "Permissions Granted")
        }
    }

    private val mServiceConnection = object : ServiceConnection {

        @RequiresApi(Build.VERSION_CODES.ECLAIR)
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!bluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            bluetoothLeService!!.connect(mibandDevice?.address)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothLeService = null
        }
    }

    /**
     * BluetoothLeService 에서 날린 브로드캐스트를 여기서 받음.
     */
    val b = BluetoothLeService()
    private val mGattUpdateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action){
                ACTION_GATT_CONNECTED -> {
                    mConnected = true
                    invalidateOptionsMenu()
                }
                ACTION_GATT_DISCONNECTED -> {
                    mConnected = false
                    invalidateOptionsMenu()
                }
                ACTION_DATA_AVAILABLE -> {
                    //데이터를 받을때
                    val real_step: TextView = findViewById<TextView>(R.id.real_step)
                    val distance: TextView = findViewById<TextView>(R.id.distance)
                    val recommend_step:TextView= findViewById<TextView>(R.id.recommend_step)

                    if (intent.hasExtra("totalSteps")) {
//                        real_step.append(" "+intent.getIntExtra("totalSteps", 1).toString())
                        real_step.text = "실시간 걸음수 : "+intent.getIntExtra("totalSteps", 1).toString()
                    } else {
                        Log.d("data_get", "real_step error")
                    }
                    if (intent.hasExtra("distance")) {
//                        distance.append(" "+intent.getIntExtra("distance", 1).toString())
                        distance.text = "총 거리 : "+intent.getIntExtra("distance", 1).toString()
                    } else {
                        Log.d("data_get", "distance error")
                    }

//                    recommend_step.text = "추천 걸음수 : "+b.sum_step/15
//                    Log.d("b.sum_step",b.sum_step.toString())
//                    if (intent.hasExtra("recommend_step")) {
////                        distance.append(" "+intent.getIntExtra("distance", 1).toString())
//                        recommend_step.text = "추천 걸음수 : "+intent.getIntExtra("recommend_step",1).toString()
//                    } else {
//                        Log.d("data_get", "recommend_step error")
//                    }
                }
                ACTIVITY_DATA_FETCH->{
//                    val recommend_step:TextView= findViewById<TextView>(R.id.recommend_step)
//                    recommend_step.text = "추천 걸음수 : "+intent.getIntExtra("recommend_step",1).toString()
//                    recommend_step.text = "추천 걸음수 : "+b.sum_step/15
                    //Log.d("b.sum_step",b.sum_step.toString())
                }
                ACTIVITY_DATA_SEND_OVER->{//intent action이 이거 일때
                    val recommend_step:TextView= findViewById<TextView>(R.id.recommend_step)
                    recommend_step.text = "추천 걸음수 : "+intent.getIntExtra("recommend_step",1).toString()
                    Log.d("data_get","마지막 추천 걸음수");
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    //서비스 발견했을때
                }
            }
        }
    }

    /**
     * 디바이스 스캔시 콜백
     */
    @RequiresApi(Build.VERSION_CODES.ECLAIR)
    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            if( mScanning ) {
                leDeviceListAdapter!!.addDevice(device)
                leDeviceListAdapter!!.notifyDataSetChanged()
            } else {
                if( mibandDevice != null ) { //미밴드 디바이스를 찾을 시
                    setMibandDeviceInfoView()
                }
            }
        }
    }

    /**
     * 블루투스 디바이스 스캔
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                handler?.postDelayed({
                    mScanning = false
                    bluetoothAdapter?.stopLeScan(leScanCallback)
                    if (mibandDevice == null) {
                        scanBtn?.visibility = View.VISIBLE
                        findViewById<ProgressBar>(R.id.progressBar).visibility = View.INVISIBLE
                    }
                }, SCAN_PERIOD)
                mScanning = true
                bluetoothAdapter?.startLeScan(leScanCallback)
            }
            else -> {
                mScanning = false
                bluetoothAdapter?.stopLeScan(leScanCallback)
            }
        }
    }

    /**
     * 블루투스 스캔을 통해 찾은 디바이스를 관리하기 위한 어댑터 클래스
     */
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()

        /**
         * 디바이스 이름이 Mi band인 디바이스를 찾아 mLeDevices 리스트에 집어넣음.
         */
        @RequiresApi(Build.VERSION_CODES.ECLAIR)
        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                if(device!=null){
                    if(device.name!=null){
                        if(device.name.contains("Mi Band")){
                            mLeDevices.add(device)
                            mibandDevice = device;
                            setMibandDeviceInfoView()
                            scanLeDevice(false)
                        }
                    }
                }
            }
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            TODO("Not yet implemented")
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if( isBindedService  ) unbindService(mServiceConnection)
        bluetoothLeService = null
    }

    companion object {

        @JvmField var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        @JvmField var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(ACTION_GATT_CONNECTED)
            intentFilter.addAction(ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(ACTION_DATA_AVAILABLE)
            intentFilter.addAction(ACTIVITY_DATA_FETCH)
            intentFilter.addAction(ACTIVITY_DATA_SEND_OVER)
            return intentFilter
        }
    }
}
