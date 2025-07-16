package dynastxu.zijingurpviewer.ui.grades;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.Map;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.databinding.FragmentGradesAllBinding;
import dynastxu.zijingurpviewer.global.GlobalState;

public class GradesAllFragment extends Fragment {
    private FragmentGradesAllBinding binding;
    private GradesAllViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(GradesAllViewModel.class);
        binding = FragmentGradesAllBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LinearLayout loadingLayout = binding.loading;
        final LinearLayout errorLayout = binding.error;
        final LinearLayout contentLayout = binding.content;
        final LinearLayout gradesList = binding.gradesList;
        final TextView errorText = binding.errorText;

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingLayout.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);
                contentLayout.setVisibility(View.GONE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });

        viewModel.getLoadResult().observe(getViewLifecycleOwner(), loadResult -> {
            if (loadResult == R.string.empty) return;
            errorText.setText(loadResult);
            errorLayout.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);
        });

        viewModel.getAllGrades().observe(getViewLifecycleOwner(), allGrades -> {
            if (allGrades == null) return;
            gradesList.removeAllViews();
            for (Map<String, Object> grade : allGrades) {
                final View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.grades_card_view, gradesList, false);

                final TextView tvTitle = cardView.findViewById(R.id.grades_title);
                final TextView tvInfo = cardView.findViewById(R.id.grades_info);
                final LinearLayout llGradesList = cardView.findViewById(R.id.grades_list);

                tvTitle.setText((CharSequence) grade.get("title"));
                tvInfo.setText((CharSequence) grade.get("credit_info"));

                List<Map<String, String>> courses = (List<Map<String, String>>) grade.get("courses");
                assert courses != null;
                for (Map<String, String> course: courses) {
                    final View line = LayoutInflater.from(llGradesList.getContext()).inflate(R.layout.grades_line_view, gradesList, false);

                    TextView tvCourseName = line.findViewById(R.id.course_name);
                    TextView tvCourseScore = line.findViewById(R.id.course_score);

                    tvCourseName.setText(course.get("课程名"));
                    tvCourseScore.setText(course.get("成绩"));

                    llGradesList.addView(line);
                }
                gradesList.addView(cardView);
            }
            contentLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final LinearLayout contentLayout = binding.content;
        final LinearLayout errorLayout = binding.error;
        final LinearLayout loadingLayout = binding.loading;

        if (GlobalState.getInstance().isLogin()) {
            contentLayout.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.GONE);
            viewModel.fetchAllGrades();
        } else {
            contentLayout.setVisibility(View.GONE);
            errorLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
