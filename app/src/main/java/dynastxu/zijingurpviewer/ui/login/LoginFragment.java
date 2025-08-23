package dynastxu.zijingurpviewer.ui.login;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.databinding.FragmentLoginBinding;
import dynastxu.zijingurpviewer.global.GlobalState;
import dynastxu.zijingurpviewer.network.AccessPath;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private LoginViewModel loginViewModel;
    private boolean isMaintenance = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        loginViewModel.init(requireContext());
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ImageButton captchaImage = binding.captchaImageButton;
        final EditText accountInput = binding.usernameEditText;
        final EditText passwordInput = binding.passwordEditText;
        final EditText captchaInput = binding.captchaEditText;
        final EditText routeInput = binding.routeEditText;
        final EditText VSGSESSIONIDInput = binding.VSGSESSIONIDEditText;
        final Button loginBtn = binding.loginButton;
        final Button logoutBtn = binding.logoutButton;
        final TextView loginResultTextView = binding.loginResultTextView;
        final TextView usernameTextView = binding.usernameTextView;
        final TextView IDTextView = binding.IDTextView;
        final TextView maintenanceTextView = binding.maintenanceTextView;
        final LinearLayout loginLayout = binding.login;
        final LinearLayout logoutLayout = binding.logout;
        final LinearLayout captchaLayout = binding.captchaLayout;
        final LinearLayout loadingLayout = binding.loadingLayout;
        final LinearLayout loginElements = binding.loginElements;
        final Spinner accessPathSpinner = binding.accessPathSpinner;
        final Spinner accessPathSpinnerII = binding.accessPathSpinner2;
        final ProgressBar userInfoProgressBar = binding.userInfoProgressBar;

        accessPathSpinner.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.accessPathSpinnerOptions, android.R.layout.simple_spinner_item));
        accessPathSpinnerII.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.accessPathSpinnerOptionsII, android.R.layout.simple_spinner_item));


        // 监听验证码图片更新
        loginViewModel.getCaptchaImage().observe(getViewLifecycleOwner(), bitmap -> {
            if (bitmap != null) {
                captchaImage.setImageBitmap(bitmap);
                Log.d("captcha", "验证码已刷新");
                if (loginViewModel.getCAPTCHAPreFilled()) {
                    captchaInput.setAutofillHints(getString(R.string.identifying));
                    // 使用Tesseract OCR识别验证码
                    recognizeTextWithTesseract(bitmap, recognizedText -> {
                        if (recognizedText != null && !recognizedText.isEmpty()) {
                            captchaInput.setText(recognizedText);
                        }
                    });
                }
            } else {
                Log.w("captcha", "验证码已刷新，但为 null");
            }
        });

        // 监听登录结果
        loginViewModel.getLoginResult().observe(getViewLifecycleOwner(), result -> {
            if (result == R.string.empty) {
                loginResultTextView.setText("");
                loginResultTextView.setVisibility(View.GONE);
                return;
            } else {
                loginResultTextView.setText(getString(result));
                loginResultTextView.setVisibility(View.VISIBLE);
            }
            if (result == R.string.login_success) {
                loginResultTextView.setVisibility(View.GONE);
                loginLayout.setVisibility(View.GONE);
                logoutLayout.setVisibility(View.VISIBLE);
                return;
            }
            if (result == R.string.website_maintenance) {
                isMaintenance = true;
                maintenanceTextView.setVisibility(View.VISIBLE);
                loginElements.setVisibility(View.GONE);
                return;
            } else {
                isMaintenance = false;
                maintenanceTextView.setVisibility(View.GONE);
                loginElements.setVisibility(View.VISIBLE);
            }
            if (result == R.string.login_vpn_success){
                accountInput.setVisibility(View.VISIBLE);
                passwordInput.setVisibility(View.VISIBLE);
                routeInput.setVisibility(View.GONE);
                VSGSESSIONIDInput.setVisibility(View.GONE);
                captchaLayout.setVisibility(View.VISIBLE);

                loginResultTextView.setVisibility(View.VISIBLE);

                passwordInput.setText("");

                String route = GlobalState.getInstance().getRoute();
                String VSG_SESSIONID = GlobalState.getInstance().getVSG_SESSIONID();
                String JSESSIONID = GlobalState.getInstance().getJSESSIONID();
                loginViewModel.fetchCaptcha(route, VSG_SESSIONID, JSESSIONID);
                return;
            }
            if (result == R.string.get_captcha_failed) {
                loginResultTextView.setVisibility(View.VISIBLE);
                return;
            }
        });

        // 监听加载状态
        loginViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingLayout.setVisibility(View.VISIBLE);
                loginElements.setVisibility(View.GONE);
            } else {
                loadingLayout.setVisibility(View.GONE);
                if (!isMaintenance) {
                    loginElements.setVisibility(View.VISIBLE);
                }
            }
        });

        loginViewModel.getIsLoadingUserInfo().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                userInfoProgressBar.setVisibility(View.VISIBLE);
                usernameTextView.setVisibility(View.GONE);
                IDTextView.setVisibility(View.GONE);
            } else {
                userInfoProgressBar.setVisibility(View.GONE);
                usernameTextView.setVisibility(View.VISIBLE);
                IDTextView.setVisibility(View.VISIBLE);
            }
        });

        // 监听登录状态
        loginViewModel.getIsLogging().observe(getViewLifecycleOwner(), isLogging -> {
            if (isLogging) {
                accountInput.setEnabled(false);
                passwordInput.setEnabled(false);
                routeInput.setEnabled(false);
                VSGSESSIONIDInput.setEnabled(false);
                captchaInput.setEnabled(false);
                captchaImage.setEnabled(false);

                loginBtn.setText(R.string.logging_in);
                loginBtn.setEnabled(false);
            } else {
                accountInput.setEnabled(true);
                passwordInput.setEnabled(true);
                routeInput.setEnabled(true);
                VSGSESSIONIDInput.setEnabled(true);
                captchaInput.setEnabled(true);
                captchaImage.setEnabled(true);

                loginBtn.setEnabled(true);
                loginBtn.setText(R.string.login);
            }
        });

        // 监听用户信息
        loginViewModel.getID().observe(getViewLifecycleOwner(), usernameTextView::setText);
        loginViewModel.getUsername().observe(getViewLifecycleOwner(), IDTextView::setText);

        // 校内/校外访问选择触发器
        accessPathSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selectedValue = parent.getItemAtPosition(position).toString();
                if (selectedValue.equals(getString(R.string.on_campus_access))) {
                    accountInput.setVisibility(View.VISIBLE);
                    passwordInput.setVisibility(View.VISIBLE);
                    routeInput.setVisibility(View.GONE);
                    VSGSESSIONIDInput.setVisibility(View.GONE);
                    captchaLayout.setVisibility(View.VISIBLE);

                    accessPathSpinnerII.setVisibility(View.VISIBLE);

                    GlobalState.getInstance().setAccessPath(AccessPath.OnCampus);
                    loginViewModel.fetchCaptcha();
                } else if (selectedValue.equals(getString(R.string.off_campus_access))) {
//                    // 使用 cookies
//                    accountInput.setVisibility(View.GONE);
//                    passwordInput.setVisibility(View.GONE);
//                    routeInput.setVisibility(View.VISIBLE);
//                    VSGSESSIONIDInput.setVisibility(View.VISIBLE);
//                    captchaLayout.setVisibility(View.GONE);

                    // 使用账号密码
                    accountInput.setVisibility(View.VISIBLE);
                    passwordInput.setVisibility(View.VISIBLE);
                    routeInput.setVisibility(View.GONE);
                    VSGSESSIONIDInput.setVisibility(View.GONE);
                    captchaLayout.setVisibility(View.GONE);

                    accessPathSpinnerII.setVisibility(View.GONE);

                    GlobalState.getInstance().setAccessPath(AccessPath.OffCampus);
                    loginViewModel.fetch();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 刷新验证码
        captchaImage.setOnClickListener(v -> {
            String route = GlobalState.getInstance().getRoute();
            String VSG_SESSIONID = GlobalState.getInstance().getVSG_SESSIONID();
            String JSESSIONID = GlobalState.getInstance().getJSESSIONID();
            if (GlobalState.getInstance().isLoginVPN()) {
                loginViewModel.fetchCaptcha(route, VSG_SESSIONID, JSESSIONID);
            } else {
                loginViewModel.fetchCaptcha();
            }
        });

        // 登录按钮
        loginBtn.setOnClickListener(v -> {
            String account = accountInput.getText().toString();
            String password = passwordInput.getText().toString();
            String captcha = captchaInput.getText().toString();
//            String route = routeInput.getText().toString();
//            String VSG_SESSIONID = VSGSESSIONIDInput.getText().toString();

            if (accessPathSpinner.getSelectedItem().toString().equals(getString(R.string.on_campus_access))) {
                if (!account.isEmpty() && !password.isEmpty() && !captcha.isEmpty()) {
                    loginViewModel.performLogin(account, password, captcha);
                }
            } else if (accessPathSpinner.getSelectedItem().toString().equals(getString(R.string.off_campus_access))) {
                if (GlobalState.getInstance().isLoginVPN()) {
                    if (!account.isEmpty() && !password.isEmpty() && !captcha.isEmpty()){
                        String route = GlobalState.getInstance().getRoute();
                        String VSG_SESSIONID = GlobalState.getInstance().getVSG_SESSIONID();
                        String JSESSIONID = GlobalState.getInstance().getJSESSIONID();
                        loginViewModel.performLogin(route, VSG_SESSIONID, JSESSIONID, account, password, captcha);
                    }
                } else {
//                    // 使用 cookies
//                    if (!route.isEmpty() && !VSG_SESSIONID.isEmpty()) {
//                        loginViewModel.performLoginWithCookies(route, VSG_SESSIONID);
//                    }
                    // 使用账号密码
                    if (!account.isEmpty() && !password.isEmpty()) {
                        loginViewModel.performLogin(account, password);
                    }
                }
            }
        });

        // 登出按钮
        logoutBtn.setOnClickListener(v -> {
            loginResultTextView.setText("");
            loginLayout.setVisibility(View.VISIBLE);
            logoutLayout.setVisibility(View.GONE);
            passwordInput.setText("");
            captchaInput.setText("");
            accessPathSpinner.setSelection(0);
            GlobalState.getInstance().clearLogin();
        });


        if (GlobalState.getInstance().isLogin()) {
            loginLayout.setVisibility(View.GONE);
            logoutLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void recognizeTextWithTesseract(Bitmap bitmap, OnTextRecognizedListener listener) {
        new Thread(() -> {
            TessBaseAPI tessBaseAPI = null;
            try {
                // 初始化Tesseract
                tessBaseAPI = new TessBaseAPI();

                // 设置Tesseract数据路径
                String dataPath = requireContext().getFilesDir() + "/tesseract/";

                // 检查并创建目录
                File dir = new File(dataPath + "tessdata/");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // 从assets复制语言数据文件（只需执行一次）
                String[] languageFiles = {"eng.traineddata"};
                for (String languageFile : languageFiles) {
                    File file = new File(dataPath + "tessdata/" + languageFile);
                    if (!file.exists()) {
                        try {
                            InputStream in = requireContext().getAssets().open("tessdata/" + languageFile);
                            OutputStream out = new FileOutputStream(file);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            in.close();
                            out.close();
                        } catch (IOException e) {
                            Log.e("Tesseract", "无法复制语言文件: " + e.getMessage());
                            requireActivity().runOnUiThread(() -> listener.onTextRecognized(null));
                            return;
                        }
                    }
                }

                // 初始化Tesseract引擎
                if (tessBaseAPI.init(dataPath, "eng")) {
                    // 设置识别参数 - 只识别数字和字母
                    tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
                    tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?");

                    // 设置图像并识别
                    tessBaseAPI.setImage(bitmap);
                    String recognizedText = tessBaseAPI.getUTF8Text();

                    // 清理识别结果
                    String cleanedText = recognizedText.trim().replaceAll("[^a-zA-Z0-9]", "");

                    Log.d("Tesseract", "识别结果: " + cleanedText);

                    // 回到主线程更新UI
                    requireActivity().runOnUiThread(() -> listener.onTextRecognized(cleanedText));
                } else {
                    Log.e("Tesseract", "初始化失败");
                    requireActivity().runOnUiThread(() -> listener.onTextRecognized(null));
                }
            } catch (Exception e) {
                Log.e("Tesseract", "识别失败: " + e.getMessage());
                requireActivity().runOnUiThread(() -> listener.onTextRecognized(null));
            } finally {
                if (tessBaseAPI != null) {
                    tessBaseAPI.end();
                }
            }
        }).start();
    }

    interface OnTextRecognizedListener {
        void onTextRecognized(String recognizedText);
    }
}
