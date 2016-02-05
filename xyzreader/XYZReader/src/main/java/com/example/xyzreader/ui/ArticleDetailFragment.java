package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import javax.security.auth.callback.Callback;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,AppBarLayout.OnOffsetChangedListener {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;


    private int mTopInset;
    private View mPhotoContainerView;
    public ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private CollapsingToolbarLayout mCTlbr;
    private String mTitle;
    private boolean mIsTransitioning;
    private Toolbar mTlbr;
    private boolean mIsTitleShown = true;
    private int mMaxScrollSize;

    private static final String ARG_POSITION = "transition_string_position";
    private static final String ARG_CURRENTPOSITION = "transition_current_position";
      private int mItemPosition;;
    private int mStartingPosition;
    private long mBackgroundImageFadeMillis;
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */

    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId,int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_CURRENTPOSITION, position);
        arguments.putInt(ARG_POSITION, startingPosition);
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        if (getArguments().containsKey(ARG_POSITION)) {
            mStartingPosition = getArguments().getInt(ARG_POSITION);
        }
        mItemPosition=getArguments().getInt(ARG_CURRENTPOSITION);
        mIsTransitioning = savedInstanceState == null && mStartingPosition == mItemPosition;

    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (mMaxScrollSize == 0)
            mMaxScrollSize = appBarLayout.getTotalScrollRange();


        if (mMaxScrollSize + i == 0) {
            mCTlbr.setTitle(mTitle);
            mIsTitleShown = true;
            mTlbr.setBackgroundColor(mMutedColor);
        } else if(mIsTitleShown) {
            mCTlbr.setTitle("");
            mIsTitleShown = false;
            mTlbr.setBackgroundColor(getResources().getColor(R.color.trans));
        }
//Log.e(TAG,"scrollsize "+mMaxScrollSize);
     //   Log.e(TAG,"vericaloffset "+i);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView=(ImageView) mRootView.findViewById(R.id.photo);

       // mRootView.findViewById(R.id.photo).setTransitionName(getString(R.string.transition_image) + String.valueOf(mAlbumPosition));
        mCTlbr = (CollapsingToolbarLayout) mRootView.findViewById(R.id.detail_collapsing);

        AppBarLayout appbarLayout = (AppBarLayout) mRootView.findViewById(R.id.detail_appbar);
        appbarLayout.addOnOffsetChangedListener(this);




        mTlbr = (Toolbar) mRootView.findViewById(R.id.detail_toolbar);

        mTlbr.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getActivity().onBackPressed();
            }
        });

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));


            }
        });

        bindViews();

        return mRootView;


    }


    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    ImageView getAlbumImage() {

        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
       return null;

    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }


    private void startPostponedEnterTransition() {
        if (mItemPosition == mStartingPosition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                        getActivity().startPostponedEnterTransition();
                        return true;
                    }
                });
            }
        }

    }

    /*public void scheduleStartPostponedTransition() {
        if (mAlbumPosition == mStartingPosition) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                            getActivity(). startPostponedEnterTransition();
                            return true;
                        }
                    });
        }
    }*/
    /*private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }*/

   /* static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }*/

   /* static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }*/

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        mPhotoView.setTransitionName(getString(R.string.transition_image) + String.valueOf(mItemPosition));


        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(mTitle);
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));


            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            //TODO: Replace thumb with photo url. Usingthimb for shared transition testing purpose.



            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.THUMB_URL), new ImageLoader.ImageListener() {

                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startPostponedEnterTransition();
                    }

                    Bitmap bitmap = imageContainer.getBitmap();
                    if (bitmap != null) {

                        Palette p = Palette.from(bitmap).generate();
                        mMutedColor = p.getDarkMutedColor(0xFF333333);
                        mPhotoView.setImageBitmap(imageContainer.getBitmap());
                        mRootView.findViewById(R.id.meta_bar)
                                .setBackgroundColor(mMutedColor);



                    }
                }


                        @Override
                public void onErrorResponse(VolleyError volleyError) {



                }
            });

        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

       // ((ArticleDetailActivity)getActivity()).scheduleStartPostponedTransition(mPhotoView);
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();

    }

}
