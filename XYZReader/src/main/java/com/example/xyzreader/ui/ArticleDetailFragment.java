package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment {

    private static final String TAG = ArticleDetailFragment.class.getSimpleName();

    public static final String ARTICLE_TEXT = "article_text";

//    private Cursor mCursor;
//    private long mItemId;
    private View mRootView;
    private String articleText;

    @BindView(R.id.article_body)
    TextView articleBody;

    private Unbinder unbinder;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(String article) {
        Bundle arguments = new Bundle();
        arguments.putString(ARTICLE_TEXT, article);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        unbinder = ButterKnife.bind(this, mRootView);

        if (getArguments() != null && getArguments().containsKey(ARTICLE_TEXT)) {
            articleText = getArguments().getString(ARTICLE_TEXT);
        }

        bindViews();

        return mRootView;
    }

    // Binding reset
    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (!TextUtils.isEmpty(articleText)) {
            articleBody.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

            articleBody.setText(Html.fromHtml(formattedText(articleText).substring(0,2000)));

        } else {
            mRootView.setVisibility(View.GONE);
            articleBody.setText("N/A");
        }
    }

    // Helper method to format the article body
    private String formattedText(String originalText) {
        String text1 = originalText.replaceAll("(\r\n\r\n)", "<br><br>");
        String text2 = text1.replaceAll("(\r\n)", " ");
        String text3 = text2.replaceAll("--", "");
        String text4 = text3.replaceAll("[\\[\\]\\(\\)]", "");
        String text5 = text4.replaceAll("\\*", "");
        return text5;
    }
}
