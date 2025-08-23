package dynastxu.zijingurpviewer.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;

    private View settingCAPTCHAPreFilled;
    private View settingSavePassword;
    private View settingAutoLogin;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        viewModel.init(requireContext()); // 初始化 SharedPreferences
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final LinearLayout generalLayout = binding.settingsGeneralLayout;

        settingCAPTCHAPreFilled = getSettingItemView(getString(R.string.setting_captcha_pre_filled), getString(R.string.setting_captcha_pre_filled_description));
        settingSavePassword = getSettingItemView(getString(R.string.setting_save_password));
        settingAutoLogin = getSettingItemView(getString(R.string.setting_auto_login), String.format(getString(R.string.setting_auto_login_description), getString(R.string.setting_captcha_pre_filled), getString(R.string.setting_save_password)));

        generalLayout.addView(settingCAPTCHAPreFilled);
        generalLayout.addView(settingSavePassword);
        generalLayout.addView(settingAutoLogin);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SwitchCompat switchCAPTCHAPreFilled = getSettingSwitch(settingCAPTCHAPreFilled);
        final SwitchCompat switchSavePassword = getSettingSwitch(settingSavePassword);
        final SwitchCompat switchAutoLogin = getSettingSwitch(settingAutoLogin);

        switchCAPTCHAPreFilled.setChecked(viewModel.getCAPTCHAPreFilled());
        switchCAPTCHAPreFilled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setCAPTCHAPreFilled(isChecked);
            if (!isChecked) {
                switchAutoLogin.setChecked(false);
            }
        });

        switchSavePassword.setChecked(viewModel.getSavePassword());
        switchSavePassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setSavePassword(isChecked);
            if (!isChecked) {
                switchAutoLogin.setChecked(false);
            }
        });

        switchAutoLogin.setChecked(viewModel.getAutoLogin());
        switchAutoLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setAutoLogin(isChecked);
            if (isChecked) {
                switchCAPTCHAPreFilled.setChecked(true);
                switchSavePassword.setChecked(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @NonNull
    private View getSettingItemView(String name, String description) {
        final View itemView = LayoutInflater.from(getContext()).inflate(R.layout.settings_item_view, binding.getRoot(), false);
        final TextView tvName = itemView.findViewById(R.id.setting_item_name);
        final TextView tvDescription = itemView.findViewById(R.id.setting_item_description);
        tvName.setText(name);
        tvDescription.setText(description);
        return itemView;
    }

    @NonNull
    private View getSettingItemView(String name) {
        return getSettingItemView(name, name);
    }

    private SwitchCompat getSettingSwitch(@NonNull View itemView) {
        return itemView.findViewById(R.id.setting_item_switch);
    }
}
