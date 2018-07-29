package com.example.bea.shoppy.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.bea.shoppy.R;
import com.example.bea.shoppy.data.ShoppyContract;

class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context mContext;
    private Cursor mCursor;

    public WidgetRemoteViewsFactory(Context applicationContext, Intent intent) {
        mContext = applicationContext;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        final long token = Binder.clearCallingIdentity();
        try {
            Uri SHOPPY_URI = ShoppyContract.BASE_CONTENT_URI.buildUpon().appendPath(ShoppyContract.PATH_SHOPPY).build();
            if (mCursor != null) mCursor.close();
            mCursor = mContext.getContentResolver().query(
                    SHOPPY_URI,
                    null,
                    null,
                    null,
                    null
            );
        } finally {
            Binder.restoreCallingIdentity(token);
        }


    }

//        if (mCursor != null) {
//        mCursor.close();
//        }
//
//final long identityToken = Binder.clearCallingIdentity();
//        Uri uri = Uri.parse(ShoppyContract.PATH_SHOPPY);
//        mCursor = mContext.getContentResolver().query(uri,
//        null,
//        null,
//        null,
//        ShoppyContract.ShoppyEntry._ID + " DESC");
//
//        Binder.restoreCallingIdentity(identityToken);
//
//        }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION ||
                mCursor == null || !mCursor.moveToPosition(position)) {
            return null;
        }

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.collection_widget_list_item);
        rv.setTextViewText(R.id.widgetItemTaskNameLabel, mCursor.getString(1));

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(WidgetProvider.EXTRA_LABEL, mCursor.getString(1));
        rv.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return mCursor.moveToPosition(position) ? mCursor.getLong(0) : position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}