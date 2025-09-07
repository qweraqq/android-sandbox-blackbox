package top.niunaijun.blackboxa.view.main

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.hello.sandbox.SandBoxCore
import java.io.File
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.databinding.ActivityMainBinding
import top.niunaijun.blackboxa.util.Resolution
import top.niunaijun.blackboxa.util.inflate
import top.niunaijun.blackboxa.view.apps.AppsFragment
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.fake.FakeManagerActivity
import top.niunaijun.blackboxa.view.setting.SettingActivity

class MainActivity : LoadingActivity() {

  private val viewBinding: ActivityMainBinding by inflate()

  private lateinit var mViewPagerAdapter: ViewPagerAdapter

  private val fragmentList = mutableListOf<AppsFragment>()

  private var currentUser = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(viewBinding.root)
    initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)
    initViewPager()
    initToolbarSubTitle()
  }

  private fun initToolbarSubTitle() {
    updateUserRemark(0)
    // hack code
    viewBinding.toolbarLayout.toolbar.getChildAt(1).setOnClickListener {
      MaterialDialog(this).show {
        title(res = R.string.userRemark)
        input(
          hintRes = R.string.userRemark,
          prefill = viewBinding.toolbarLayout.toolbar.subtitle
        ) { _, input ->
          AppManager.mRemarkSharedPreferences.edit {
            putString("Remark$currentUser", input.toString())
            viewBinding.toolbarLayout.toolbar.subtitle = input
          }
        }
        positiveButton(res = R.string.done)
        negativeButton(res = R.string.cancel)
      }
    }
  }

  private fun initViewPager() {

    val userList = SandBoxCore.get().users
    userList.forEach { fragmentList.add(AppsFragment.newInstance(it.id)) }

    currentUser = userList.firstOrNull()?.id ?: 0
    fragmentList.add(AppsFragment.newInstance(userList.size))

    mViewPagerAdapter = ViewPagerAdapter(this)
    mViewPagerAdapter.replaceData(fragmentList)
    viewBinding.viewPager.adapter = mViewPagerAdapter
    viewBinding.dotsIndicator.setViewPager2(viewBinding.viewPager)
    viewBinding.viewPager.registerOnPageChangeCallback(
      object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          currentUser = fragmentList[position].userID
          updateUserRemark(currentUser)
          showFloatButton(true)
        }
      }
    )
  }



  private fun listener(dlid: Long, context: Context, filename: String) {

    val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    val broadcastReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
          if (dlid == id) {
            install(context, File(filename))
            context.unregisterReceiver(this)
          }
        }
      }

    context.registerReceiver(broadcastReceiver, intentFilter)
  }

  fun install(context: Context, file: File) {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.getPackageManager().canRequestPackageInstalls()
    ) {
      startInstallPermissionSettingActivity(context)
    }
    try {
      if (file.exists()) {
        val promptInstall =
          Intent()
            .setAction(android.content.Intent.ACTION_VIEW)
            .setDataAndType(
              getInstallApkUri(context, file),
              "application/vnd.android.package-archive"
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
          promptInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(promptInstall)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /** 根据版本获取安装路径uri */
  private fun getInstallApkUri(context: Context, file: File): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      FileProvider.getUriForFile(context, "files." + context.packageName, file)
    } else {
      Uri.fromFile(file)
    }
  }

  /** 跳转到设置-允许安装未知来源-页面 */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun startInstallPermissionSettingActivity(context: Context) {
    // 注意这个是8.0新API
    val uri = Uri.parse("package:" + context.packageName)
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }
  fun showFloatButton(show: Boolean) {
    val tranY: Float = Resolution.convertDpToPixel(120F, App.getContext())
    val time = 200L
    if (show) {
      viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time).start()
    } else {
      viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time).start()
    }
  }

  fun scanUser() {
    val userList = SandBoxCore.get().users

    if (fragmentList.size == userList.size) {
      fragmentList.add(AppsFragment.newInstance(fragmentList.size))
    } else if (fragmentList.size > userList.size + 1) {
      fragmentList.removeLast()
    }

    mViewPagerAdapter.notifyDataSetChanged()
  }

  private fun updateUserRemark(userId: Int) {
    var remark = AppManager.mRemarkSharedPreferences.getString("Remark$userId", "User $userId")
    if (remark.isNullOrEmpty()) {
      remark = "User $userId"
    }

    viewBinding.toolbarLayout.toolbar.subtitle = remark
  }

  private val apkPathResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == RESULT_OK) {
        it.data?.let { data ->
          val userId = data.getIntExtra("userID", 0)
          val source = data.getStringExtra("source")
          val fromSystem = data.getBooleanExtra("fromSystem", true)
          if (source != null) {
            fragmentList[userId].installApk(source, fromSystem)
          }
        }
      }
    }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item?.itemId) {
      R.id.main_git -> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/FBlackBox/BlackBox"))
        startActivity(intent)
      }
      R.id.main_setting -> {
        SettingActivity.start(this)
      }
      R.id.main_tg -> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/fvblackbox"))
        startActivity(intent)
      }
      R.id.fake_location -> {
        //                toast("Still Developing")
        val intent = Intent(this, FakeManagerActivity::class.java)
        intent.putExtra("userID", 0)
        startActivity(intent)
      }
    }

    return true
  }

  companion object {
    fun start(context: Context) {
      val intent = Intent(context, MainActivity::class.java)
      context.startActivity(intent)
    }
  }
}
