package com.hello.sandbox.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hello.sandbox.Constant
import com.hello.sandbox.common.util.ClipboardUtil
import com.hello.sandbox.common.util.ToastUtil
import com.hello.sandbox.common.util.ViewUtil
import com.hello.sandbox.ui.WebviewAct
import com.hello.sandbox.ui.about.AboutAct
import com.hello.sandbox.ui.splash.LoginAct
import com.hello.sandbox.user.UserManager
import com.hello.sandbox.util.singleClickListener
import com.hello.sandbox.view.BasePopup
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.util.XPopupUtils
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.FragmentMeBinding
import top.niunaijun.blackboxa.view.setting.SettingActivity


class MeFrag : Fragment() {
  private lateinit var binding: FragmentMeBinding
  private lateinit var rlAbout: RelativeLayout
  private lateinit var tvName: TextView

  private lateinit var appInfos: ArrayList<AppInfo>
  private val SHARE_URL =
    "https://m.tantanapp.com/fep/tantan/frontend/tantan-frontend-app-pages-v2/src/pages/tool/landing/index.html?speed=true&_bid=1004060"
  companion object {
    @JvmStatic
    fun getInstance(): MeFrag {
      return MeFrag()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = FragmentMeBinding.inflate(layoutInflater)
    rlAbout = binding.rlAbout
    tvName = binding.tvName
    tvName.text = "ID:" + UserManager.instance.getUserId()
    initClickListener()
    appInfos = ArrayList()
    (requireActivity() as HomeAct).viewModel.appsLiveData.observe(this) {
      if (it != null) {
        appInfos.clear()
        appInfos.addAll(it)
      }
    }
  }

  private fun initClickListener() {
    ViewUtil.singleClickListener(rlAbout) { AboutAct.start(context!!) }
    binding.rlQuestion.singleClickListener {
      val intent: Intent = Intent(requireContext(), SettingActivity::class.java)
      startActivity(intent)
    }
    binding.llHelp.singleClickListener {
      WebviewAct.start(requireContext(), Constant.URL_APP_FEEDBACK, "问题反馈")
    }
    binding.rlShare.singleClickListener {
      ClipboardUtil.copy("$SHARE_URL 请打开此链接下载秘盒空间")
      ToastUtil.message("链接复制成功，请到微信、qq粘贴分享")
    }

    binding.rlLogout.singleClickListener {
      val logoutPopup =
        BasePopup(
          requireContext(),
          getString(R.string.logout_popup_title),
          getString(R.string.logout_popup_description),
          { unInstallAllAndLogout() },
          {},
          getString(R.string.logout_popup_confirm),
          getString(R.string.logout_popup_cancel)
        )
      XPopup.Builder(requireContext())
        .moveUpToKeyboard(false)
        .maxWidth(
          XPopupUtils.getScreenWidth(requireContext()) - XPopupUtils.dp2px(requireContext(), 40f)
        )
        .isDestroyOnDismiss(true)
        .dismissOnTouchOutside(false)
        .dismissOnBackPressed(false)
        .asCustom(this.let { logoutPopup })
        .show()
    }
  }

  private fun unInstallAllAndLogout() {
    (requireActivity() as HomeAct).showLoading()
    (requireActivity() as HomeAct).viewModel.allUninstallData.observe(this) {
      appInfos.clear()
      UserManager.instance.logout()
      LoginAct.start(requireContext())
      activity?.finish()
    }
    (requireActivity() as HomeAct).viewModel.unInstallApks(appInfos, HomeAct.userID)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return binding.root
  }
}
