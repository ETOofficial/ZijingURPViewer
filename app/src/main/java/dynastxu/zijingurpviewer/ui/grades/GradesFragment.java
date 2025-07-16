package dynastxu.zijingurpviewer.ui.grades;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.databinding.FragmentGradesBinding;
import dynastxu.zijingurpviewer.global.GlobalState;

public class GradesFragment extends Fragment {
    private FragmentGradesBinding binding;
    private GradesViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(GradesViewModel.class);
        binding = FragmentGradesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final CardView allGrades = binding.checkAllGrades;
        final CardView failingGrades = binding.checkFailingGrades;

        allGrades.setOnClickListener(v -> {
            NavController nc = Navigation.findNavController(view);
            nc.navigate(R.id.nav_grades_all);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final LinearLayout contentLayout = binding.content;
        final LinearLayout errorLayout = binding.error;
        final TextView errorText = binding.errorText;

        if (GlobalState.getInstance().isLogin()) {
            contentLayout.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);
        } else {
            errorText.setText(R.string.notLoggedIn);
            contentLayout.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
