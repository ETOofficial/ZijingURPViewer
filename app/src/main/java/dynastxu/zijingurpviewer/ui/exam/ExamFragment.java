package dynastxu.zijingurpviewer.ui.exam;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import dynastxu.zijingurpviewer.R;
import dynastxu.zijingurpviewer.databinding.FragmentExamBinding;
import dynastxu.zijingurpviewer.global.GlobalState;

public class ExamFragment extends Fragment {
    private FragmentExamBinding binding;
    private ExamViewModel examViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        examViewModel = new ViewModelProvider(this).get(ExamViewModel.class);
        binding = FragmentExamBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final LinearLayout contentLayout = binding.content;
        final LinearLayout errorLayout = binding.error;
        final LinearLayout loadingLayout = binding.loading;
        final LinearLayout examList = binding.examList;
        final TextView errorText = binding.errorText;

        examViewModel.getExamData().observe(getViewLifecycleOwner(), examData -> {
            Log.d("ExamFragment", "数据更新 examData: " + examData);
            if (examData == null) return;
            examList.removeAllViews();
            for (List<String> exam : examData) {
                final View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.exam_card_view, examList, false);

                final TextView tvTitle = cardView.findViewById(R.id.exam_title);
                final TextView tvName = cardView.findViewById(R.id.exam_name);
                final TextView tvTime = cardView.findViewById(R.id.exam_time);
                final TextView tvAddress = cardView.findViewById(R.id.exam_address);
                final TextView tvTicket = cardView.findViewById(R.id.exam_ticket);

                tvTitle.setOnClickListener(v -> {
                    if (tvName.getVisibility() == View.GONE) {
                        tvName.setVisibility(View.VISIBLE);
                        tvTime.setVisibility(View.VISIBLE);
                        tvAddress.setVisibility(View.VISIBLE);
                        tvTicket.setVisibility(View.VISIBLE);
                    } else {
                        tvName.setVisibility(View.GONE);
                        tvTime.setVisibility(View.GONE);
                        tvAddress.setVisibility(View.GONE);
                        tvTicket.setVisibility(View.GONE);
                    }
                });

                tvTitle.setText(exam.get(4));
                // TODO Translate
                tvName.setText(String.format("考试名称: %s", exam.get(0)));
                tvTime.setText(String.format("考试时间: 第 %s 周 星期 %s %s", exam.get(5), exam.get(6), exam.get(7)));
                tvAddress.setText(String.format("考试地点: %s %s %s", exam.get(1), exam.get(2), exam.get(3)));
                tvTicket.setText(String.format("准考证: %s 座位号: %s", exam.get(9), exam.get(8)));

                examList.addView(cardView);
            }
            contentLayout.setVisibility(View.VISIBLE);
            Log.d("ExamFragment", "数据更新完毕");
        });

        examViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingLayout.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
                errorLayout.setVisibility(View.GONE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });

        examViewModel.getLoadResult().observe(getViewLifecycleOwner(), loadResult -> {
            if (loadResult == R.string.extractExamDataFailed) {
                errorLayout.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);

                errorText.setText(loadResult);
            }
            if (loadResult == R.string.getExamDataSuccess) {
                contentLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final LinearLayout contentLayout = binding.content;
        final LinearLayout errorLayout = binding.error;
        final LinearLayout loadingLayout = binding.loading;

        if (GlobalState.getInstance().isLogin()) {
            contentLayout.setVisibility(View.GONE);
            errorLayout.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.VISIBLE);
            examViewModel.fetchExamData();
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
