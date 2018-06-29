package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    public static final String ITEM_POSITION = "item_position";

    private Cursor mCursor;
    private int mSelectedItemId;
    private String mArticleTitle;

    private MyPagerAdapter mPagerAdapter;

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.appbar)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout mCollapsingToolBar;
    @BindView(R.id.photo)
    ImageView articleImage;
    @BindView(R.id.article_container)
    FrameLayout articleContainer;
    @BindView(R.id.meta_bar)
    LinearLayout mLinearLayout;
    @BindView(R.id.article_title)
    TextView articleTitle;
    @BindView(R.id.article_byline)
    TextView articleByline;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.pager)
    ViewPager mPager;
    @BindView(R.id.share_fab)
    FloatingActionButton mFab;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        ButterKnife.bind(this);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                if (getIntent().hasExtra(ITEM_POSITION)) {
                    mSelectedItemId = getIntent().getExtras().getInt(ITEM_POSITION, 0);
                    Log.i(TAG, "mSelectedItemId = " + mSelectedItemId);
                }
//                mSelectedItemId = ItemsContract.Items.getItemId(getIntent().getData());
                Log.i(TAG, "mSelectedItemId = " + mSelectedItemId);
            }
        }

        // Set up the Up button
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = position;
//                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                mPagerAdapter.notifyDataSetChanged();
                showArticleDetails();
            }
        });

        // This code snippet is from https://stackoverflow.com/a/32724422/8646848 to display title
        // of the article only if the toolbar collapsed.
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }

                if (scrollRange + verticalOffset == 0) {
                    mCollapsingToolBar.setTitle(mArticleTitle);
                    isShow = true;

                } else if (isShow) {
                    mCollapsingToolBar.setTitle(" ");//careful there should a space between double quote otherwise it wont work
                    isShow = false;
                }
            }
        });

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareArticle();
            }
        });

        // Create a Snackbar message at the bottom
        Snackbar snackbar = Snackbar
                .make(mCoordinatorLayout, R.string.snackbar_message, Snackbar.LENGTH_LONG);
        View view = snackbar.getView();
        TextView snackbarText = view.findViewById(android.support.design.R.id.snackbar_text);
        snackbarText.setGravity(Gravity.CENTER_HORIZONTAL);
        snackbarText.setTextColor(getResources().getColor(R.color.colorAccent));
        snackbar.show();

        getLoaderManager().initLoader(0, null, this);

    }

    // Helper method to show article details
    private void showArticleDetails() {
        mCursor.moveToPosition(mSelectedItemId);

        articleByline.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mArticleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            articleTitle.setText(mArticleTitle);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                articleByline.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                articleByline.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            ImageLoaderHelper.getInstance(this).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                articleImage.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

        } else {
            articleTitle.setText("N/A");
            articleByline.setText("N/A");
            articleImage.setBackgroundColor(getResources().getColor(R.color.photo_placeholder));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i(TAG, "mSelectedItemId/onCreateLoader = " + mSelectedItemId);
        return ArticleLoader.newAllArticlesInstance(this);
//        return ArticleLoader.newInstanceForItemId(this, mSelectedItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        int position = cursor.getPosition();
        Log.i(TAG, "onLoadfinished mSelectedItemId: " + position);

        mCursor = cursor;

        showArticleDetails();

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        // mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.setCurrentItem(position);
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    // Set up animation back to ArticleListActivity if up button pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Set up animation back to ArticleListActivity if back button pressed
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }

    private class MyPagerAdapter extends android.support.v4.app.FragmentStatePagerAdapter {
        public MyPagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

//        @Override
//        public Parcelable saveState() {
//            Bundle bundle = (Bundle) super.saveState();
//            bundle.putParcelableArray("states", null); // Never maintain any states from the base class, just null it out
//            return bundle;
//        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getString(ArticleLoader.Query.BODY));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }

    // Method to share the article
    private void shareArticle() {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
    }

    // Helper method to parse published date
    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

}
