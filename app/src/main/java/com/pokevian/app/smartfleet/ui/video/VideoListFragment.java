/*
 * Copyright (c) 2014. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.ui.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.video.MediaUtils.MediaMetadata;
import com.pokevian.app.smartfleet.util.StorageUtils;
import com.pokevian.lib.common.imageloader.ImageCache.ImageCacheParams;
import com.pokevian.lib.common.imageloader.ImageExtractor;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


public class VideoListFragment extends Fragment {

    static final String TAG = "VideoListFragment";
    final Logger log = Logger.getLogger(TAG);

    private static final int CODE_PLAYER_ACTIVITY = 1;

    public static final String ARG_ROOT_DIR = "root_dir";

    private VideoListActivity mActivity;
    private ActionMode mActionMode;

    private View mContentView;
    private TextView mMessageText;
    private ListView mListView;
    private VideoListAdapter mListAdapter;
    private ArrayList<ItemWrapper> mItems;
    private LoadThread mLoadThread;
    private File mArchiveDir;

    private File mRootDir;
    private File mCurrDir;
    private int mSelectedPosition = 0;
    private int mPrevPosition = 0;

    private VideoCopyThread mVideoCopyThread;
    private VideoCopyProgressDialog mCopyProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = (VideoListActivity) getActivity();

        SettingsStore settingsStore = SettingsStore.getInstance();

        int storageIndex = settingsStore.getBlackboxStorageType().ordinal();
        File[] archiveRootDirs = StorageUtils.getExternalFilesDirs(getActivity(),
                settingsStore.getBlackboxArchiveDirName());
        mArchiveDir = archiveRootDirs[storageIndex];

        mItems = new ArrayList<>();

        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalArgumentException("no arguments");
        }

        mRootDir = (File) args.getSerializable(ARG_ROOT_DIR);
        if (mRootDir == null || !mRootDir.isDirectory()) {
            throw new IllegalArgumentException("invalid root_directory");
        }
        mCurrDir = mRootDir;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLoadThread != null) {
            mLoadThread.interrupt();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContentView == null) {
            mContentView = inflater.inflate(R.layout.fragment_video_list, null);

            mMessageText = (TextView) mContentView.findViewById(R.id.message);
            mListView = (ListView) mContentView.findViewById(R.id.video_list);
            mListAdapter = new VideoListAdapter(getActivity(), mItems);
            mListView.setAdapter(mListAdapter);

            loadItems();
        } else {
            if (mContentView.getParent() != null) {
                ((ViewGroup) mContentView.getParent()).removeView(mContentView);
            }

        }

        // clear check flags
        for (ItemWrapper item : mItems) {
            item.isChecked = false;
        }

        return mContentView;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.getLogger("VideoListF").trace("onActivityResult# requestCode=" + requestCode + " resultCode=" + resultCode + " data=" + data);
        if (requestCode == CODE_PLAYER_ACTIVITY) {
            setShowVideo(false);

            if (resultCode == Activity.RESULT_OK) {
                int deletedIndex = data.getIntExtra(VideoPlayActivity.EXTRA_DELETED, -1);
                if (VideoListActivity.TAG_ARCHIVE.equals(getTag())) {
                    if (deletedIndex != -1) {
                        deleteItem(deletedIndex);
                    } else {
                        int lastIndex = data.getIntExtra(VideoPlayActivity.EXTRA_PLAY_INDEX, 0);
                        mListView.setSelection(lastIndex);
                    }
                } else {
                    boolean isArchived = data.getBooleanExtra(VideoPlayActivity.EXTRA_ARCHIVED, false);
                    if (isArchived) {
                        // archive directory is changed -> reload
                        FragmentManager fm = mActivity.getSupportFragmentManager();
                        Fragment frag = fm.findFragmentByTag(VideoListActivity.TAG_ARCHIVE);
                        if (frag != null) {
                            ((VideoListFragment) frag).reload();
                        }
                    }
                    if (deletedIndex != -1) {
                        deleteItem(deletedIndex);
                    } else {
                        int lastIndex = data.getIntExtra(VideoPlayActivity.EXTRA_PLAY_INDEX, 0);
                        mListView.setSelection(lastIndex);
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onBackPressed() {
        if (mCurrDir.equals(mRootDir)) {
            return false;
        } else {
            // up directory
            mCurrDir = mCurrDir.getParentFile();
            mSelectedPosition = mPrevPosition;
            loadItems();
            return true;
        }
    }

    public void setMessage(int resId) {
        mMessageText.setText(resId);
        mMessageText.setVisibility(View.VISIBLE);
    }

    public void reload() {
        loadItems();
    }

    private void loadItems() {
        if (mLoadThread != null) {
            mLoadThread.interrupt();
        }

        mLoadThread = new LoadThread(getActivity());
        mLoadThread.start();
    }


    private class LoadThread extends Thread {
        private Context mContext;
        private final String mFileExt;

        public LoadThread(Context context) {
            mContext = context;
            mFileExt = Consts.DEFAULT_BLACKBOX_VIDEO_FILE_EXT;

            mListAdapter.clear();
            mListAdapter.notifyDataSetChanged();

            mMessageText.setText(R.string.video_play_msg_loading);
            mMessageText.setVisibility(View.VISIBLE);
        }

        @Override
        public void run() {
            Thread.currentThread().setName(getTag() + "_load_thread");

            ArrayList<File> files = FileUtils.dir(mCurrDir,
                    new FileFilter() {
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().endsWith(mFileExt);
                        }
                    }, new Comparator<File>() {
                        public int compare(File lhs, File rhs) {
                            if (lhs.isDirectory() && !rhs.isDirectory()) {
                                return -1;
                            } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                                return 1;
                            }
                            return rhs.getName().compareTo(lhs.getName());
                        }
                    }, false, false);

            ArrayList<ItemWrapper> items = new ArrayList<ItemWrapper>();
            for (File file : files) {
                if (isInterrupted()) {
                    log.error("video list loading thread - interrupted!");
                    break;
                }

                // delete invalid video file
                if (file.exists() && file.isFile() && file.length() <= 56/*min mp4 file size?*/) {
                    log.warn("delete video file: too small size=" + file.length());
                    File metadataFile = FileUtils.getMetadataFileFromVideoFile(file);
                    metadataFile.delete();
                    file.delete();
                    continue;
                }
                // delete empty directory
                if (file.isDirectory() && file.list().length == 0) {
                    file.delete();
                    continue;
                }

                MediaMetadata metadata = null;
                if (file.isFile()) {
                    metadata = MediaUtils.extractMetadata(file.getAbsolutePath());
                }

				/*
                //delete video file if video width is -1 or video height is -1
				if (metadata != null){
					if (metadata.videoWidth == -1 || metadata.videoHeight == -1) {
						File metadataFile = FileUtils.getMetadataFileFromVideoFile(getActivity(), file);
						metadataFile.delete();
						file.delete();
						continue;
					}
				}
				//~
				*/

                if (metadata == null) {
                    metadata = new MediaMetadata();
                }

                items.add(new ItemWrapper(file, metadata));
            }
            log.debug("video loaded: size=" + items.size());

            final ArrayList<ItemWrapper> finalItems = items;
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mItems.clear();
                    mItems.addAll(finalItems);
                    mListAdapter.notifyDataSetChanged();
                    mListView.setSelection(mSelectedPosition);

                    if (mListAdapter.isEmpty()) {
                        mMessageText.setText(R.string.video_play_msg_empty_video);
                        mMessageText.setVisibility(View.VISIBLE);
                    } else {
                        mMessageText.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void onItemClicked(int position, ItemWrapper item) {
        // finish action mode
        if (mActionMode != null) {
            mActionMode.finish();
        }

        if (item.file.isDirectory()) {
            mPrevPosition = position;
            mSelectedPosition = 0;
            mCurrDir = item.file;
            loadItems();
        } else {
            ArrayList<File> files = new ArrayList<File>();
            for (ItemWrapper i : mItems) {
                files.add(i.file);
            }

            Intent i = new Intent(getActivity(), VideoPlayActivity.class);
            i.setPackage(mActivity.getPackageName());
            i.putExtra(VideoPlayActivity.EXTRA_FILES, files);
            i.putExtra(VideoPlayActivity.EXTRA_PLAY_INDEX, position);
            i.putExtra(VideoPlayActivity.EXTRA_SHOW_ARCHIVE_MENU, !VideoListActivity.TAG_ARCHIVE.equals(getTag()));
            startActivityForResult(i, CODE_PLAYER_ACTIVITY);
            setShowVideo(true);
        }
    }

    private void onItemCheckedChagned(int position, ItemWrapper item) {
        int selectedCount = getSelectedItemCount();
        if (mActionMode == null) {
            if (selectedCount > 0) {
                boolean showArchiveMenu = !VideoListActivity.TAG_ARCHIVE.equals(getTag());
                boolean showShareMenu = true;
                // cannot archive or share a directory!
                if (item.file.isDirectory()) {
                    showArchiveMenu = false;
                    showShareMenu = false;
                }

                mActionMode = mActivity.startSupportActionMode(new ActionCallback(showArchiveMenu, showShareMenu));
                mActionMode.setTitle(String.format(getString(R.string.video_play_msg_selected), selectedCount));

            }
        } else {
            if (selectedCount > 0) {
                mActionMode.setTitle(String.format(getString(R.string.video_play_msg_selected), selectedCount));
            } else {
                mActionMode.finish();
            }
        }
    }

    private int getSelectedItemCount() {
        int selected = 0;
        for (ItemWrapper item : mItems) {
            if (item.isChecked) {
                selected++;
            }
        }
        return selected;
    }

    private class VideoListAdapter extends ArrayAdapter<ItemWrapper>
            implements View.OnClickListener, View.OnLongClickListener,
            CompoundButton.OnCheckedChangeListener {

        private final LayoutInflater mInflater;

        private final ImageExtractor mImageWorker;

        public VideoListAdapter(Context context, List<ItemWrapper> objects) {
            super(context, 0, objects);

            mInflater = LayoutInflater.from(context);

            int thumbnailWidth = context.getResources().getDimensionPixelSize(R.dimen.video_thumnail_width);
            int thumbnailHeight = context.getResources().getDimensionPixelSize(R.dimen.video_thumnail_height);
            log.debug("thumbnail: " + thumbnailWidth + "x" + thumbnailHeight);

            ImageCacheParams cacheParams = new ImageCacheParams(getActivity(), "thumbs");
            cacheParams.memoryCacheEnabled = false;
            mImageWorker = new ImageExtractor(context, thumbnailWidth, thumbnailHeight);
            mImageWorker.setLoadingImage(R.drawable.empty_thumbnail);
            mImageWorker.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);

        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            ViewHolder holder = null;

            if (v == null) {
                v = mInflater.inflate(R.layout.item_video_list, null);
                /*v.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);*/ // sherlock
                v.setOnClickListener(this);
                v.setOnLongClickListener(this);
                holder = new ViewHolder(v);
                holder.checkBox.setOnCheckedChangeListener(this);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }

            ItemWrapper item = getItem(position);
            File file = item.file;
            MediaMetadata metadata = item.metadata;

            holder.checkBox.setTag(item);
            holder.checkBox.setChecked(item.isChecked);
            if (file.isDirectory()) {
                holder.fileInfoPanel.setVisibility(View.GONE);
                holder.dirInfoPanel.setVisibility(View.VISIBLE);
                String name = TimeStringUtils.toString(TimeStringUtils.Type.LOCAL_DATE,
                        file.getName(), Consts.DEFAULT_BLACKBOX_DIR_NAME_FORMAT);
                holder.dirNameText.setText(name);
            } else {
                mImageWorker.loadImage(file.getAbsolutePath(), holder.thumbnailImage);

                holder.dirInfoPanel.setVisibility(View.GONE);
                holder.fileInfoPanel.setVisibility(View.VISIBLE);
                String name = TimeStringUtils.toString(TimeStringUtils.Type.LOCAL_DATE_TIME,
                        file.getName(), Consts.DEFAULT_BLACKBOX_FILE_NAME_FORMAT);
                holder.fileNameText.setText(name);

                if (metadata.duration > 0) {
                    holder.durationText.setText(TimeStringUtils.toString(TimeStringUtils.Type.MMSS, metadata.duration));
                } else {
                    holder.durationText.setText("--:--");
                }

                String infoText = null;
                if (metadata.videoWidth > 0 && metadata.videoHeight > 0) {
					/*infoText = String.format(Locale.getDefault(), "%s : %dx%d / %s : %dKB",
							getString(R.string.video_play_video_resolution),
							metadata.videoWidth, metadata.videoHeight,
							getString(R.string.video_play_file_size),
							file.length() / 1024);*/

                    infoText = String.format(Locale.getDefault(), "%s : %dx%d \n%s : %dKB",
                            getString(R.string.video_play_video_play_video_resolution),
                            metadata.videoWidth, metadata.videoHeight,
                            getString(R.string.video_play_file_size),
                            file.length() / 1024);
                } else {
					/*infoText = String.format(Locale.getDefault(), "%s : ---x--- / %s : %dKB",
							getString(R.string.video_play_video_resolution),
							getString(R.string.video_play_file_size),
							file.length() / 1024);*/
                    infoText = String.format(Locale.getDefault(), "%s : ---x--- \n %s : %dKB",
                            getString(R.string.video_play_video_play_video_resolution),
                            getString(R.string.video_play_file_size),
                            file.length() / 1024);
                }
                holder.fileInfoText.setText(infoText);
            }

            return v;
        }

        // View.OnClickListener
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.video_item) {
                ViewHolder holder = (ViewHolder) v.getTag();
                ItemWrapper item = (ItemWrapper) holder.checkBox.getTag();
                int position = getPosition(item);
                onItemClicked(position, item);
            }
        }

        // View.OnLongClickListener
        @Override
        public boolean onLongClick(View v) {
            int id = v.getId();
            if (id == R.id.video_item) {
                ViewHolder holder = (ViewHolder) v.getTag();
                if (holder.checkBox.getVisibility() == View.VISIBLE) {
                    holder.checkBox.setChecked(true);
                }
                return true;
            }
            return false;
        }

        // CompoundButton.OnCheckedChangeListener
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ItemWrapper item = (ItemWrapper) buttonView.getTag();
            item.isChecked = isChecked;
            int position = getPosition(item);
            onItemCheckedChagned(position, item);
        }

        class ViewHolder {
            final CheckBox checkBox;
            final View dirInfoPanel;
            final TextView dirNameText;
            final View fileInfoPanel;
            final TextView fileNameText;
            final ImageView thumbnailImage;
            final TextView durationText;
            final TextView fileInfoText;

            ViewHolder(View parent) {
                checkBox = (CheckBox) parent.findViewById(R.id.video_checkbox);
                dirInfoPanel = parent.findViewById(R.id.video_dir_info_panel);
                dirNameText = (TextView) dirInfoPanel.findViewById(R.id.video_dir_name);
                fileInfoPanel = parent.findViewById(R.id.video_file_info_panel);
                fileNameText = (TextView) fileInfoPanel.findViewById(R.id.video_file_name);
                thumbnailImage = (ImageView) fileInfoPanel.findViewById(R.id.video_thumbnail);
                durationText = (TextView) fileInfoPanel.findViewById(R.id.video_duration);
                fileInfoText = (TextView) fileInfoPanel.findViewById(R.id.video_file_info);
            }
        }

    }

    private void selectAllItems() {
        for (ItemWrapper item : mItems) {
            item.isChecked = true;
        }
        mListAdapter.notifyDataSetInvalidated();
    }

    private void unselectAllItems() {
        for (ItemWrapper item : mItems) {
            item.isChecked = false;
        }
        mListAdapter.notifyDataSetInvalidated();
    }

    private void archiveSelectedItems() {
        // build list to be copied
        final ArrayList<File> files = new ArrayList<File>();
        for (ItemWrapper item : mItems) {
            if (item.isChecked) {
                files.add(item.file);
            }
        }

        DialogFragment fragment = CopyVideoDialogFragment.newInstance(mArchiveDir, files);
        fragment.show(getChildFragmentManager(), CopyVideoDialogFragment.TAG);
    }

    public static class CopyVideoDialogFragment extends DialogFragment {

        public static final String TAG = "CopyVideoDialogFragment";

        private File mTarget;
        private ArrayList<File> mFiles;

        public static CopyVideoDialogFragment newInstance(File target, ArrayList<File> files) {
            CopyVideoDialogFragment fragment = new CopyVideoDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("target", target);
            args.putSerializable("files", files);
            fragment.setArguments(args);
            return fragment;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mTarget = (File) args.getSerializable("target");
                mFiles = (ArrayList<File>) args.getSerializable("files");
            } else {
                mTarget = (File) savedInstanceState.getSerializable("target");
                mFiles = (ArrayList<File>) savedInstanceState.getSerializable("files");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("files", mFiles);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.video_play_title_copy_video_to_archive)
                    .setMessage(R.string.video_play_msg_copy_video_to_archive)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((VideoListFragment) getParentFragment()).doCopy(mFiles, mTarget);
                        }
                    })
                    .setNegativeButton(R.string.btn_no, null)
                    .create();
        }
    }

    private void doCopy(ArrayList<File> files, File archiveDir) {
        mVideoCopyThread = new VideoCopyThread(files, archiveDir, getActivity(),
                new VideoCopyThread.Callback() {
                    public void onPreExecute(final int maxValue) {
                        mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                showCopyProgressDialog(maxValue);
                            }
                        });
                    }

                    public void onProgress(final int progress) {
                        mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                updateCopyProgressDialog(progress);
                            }
                        });
                    }

                    public void onPostExecute() {
                        mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                hideCopyProgressDialog();
                                FragmentManager fm = mActivity.getSupportFragmentManager();
                                Fragment frag = fm.findFragmentByTag(VideoListActivity.TAG_ARCHIVE);
                                if (frag != null && !frag.isRemoving()) {
                                    log.debug("reload archive dir");
                                    ((VideoListFragment) frag).reload();
                                }
                            }
                        });
                    }
                });
        mVideoCopyThread.start();
    }

    private void showCopyProgressDialog(int max) {
        mCopyProgressDialog = new VideoCopyProgressDialog(getActivity());
        mCopyProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mVideoCopyThread.interrupt();
            }
        });
        mCopyProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        mCopyProgressDialog.setMax(max);
        mCopyProgressDialog.show();

    }

    private void updateCopyProgressDialog(int value) {
        if (mCopyProgressDialog != null) {
            mCopyProgressDialog.setProgress(value);
        }
    }

    private void hideCopyProgressDialog() {
        if (mCopyProgressDialog != null) {
            try {
                mCopyProgressDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    private void shareSelectedItems() {
        boolean toastShown = false;
        ArrayList<Uri> uris = new ArrayList<Uri>();
        ContentResolver resolver = getActivity().getContentResolver();
        for (ItemWrapper item : mItems) {
            if (item.isChecked) {
                // NOTE: prevent to share a directory
                if (item.file.isFile()) {
                    Uri uri = MediaStoreUtils.retrieveVideoContentUri(resolver, item.file);
                    if (uri == null) {
                        log.info("add video file to media store");
                        uri = MediaStoreUtils.insertVideo(resolver, item.file, "video/mp4");
                    }
                    if (uri != null) {
                        uris.add(uri);

                        File metaFile = FileUtils.getMetadataFileFromVideoFile(item.file);
                        if (metaFile != null && metaFile.exists()) {
                            uris.add(Uri.fromFile(metaFile));
                        }
                    }
                } else if (!toastShown) {
                    toastShown = true;
                    Toast.makeText(getActivity(), getString(R.string.video_play_msg_cannot_share_folder), Toast.LENGTH_LONG).show();
                }
            }
        }

        if (uris.size() > 0) {
            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("video/*");
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            try {
                startActivity(Intent.createChooser(i, getString(R.string.video_play_title_share_video)));
            } catch (Exception e) {
                Toast.makeText(getActivity(), "No way to share!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void deleteItem(int index) {
        if (index < mItems.size()) {
            mItems.remove(index);
            mListAdapter.notifyDataSetChanged();

            if (mListAdapter.isEmpty()) {
                mMessageText.setText(R.string.video_play_msg_empty_video);
                mMessageText.setVisibility(View.VISIBLE);
            } else {
                mMessageText.setVisibility(View.GONE);
            }
        }
    }

    private void deleteSelectedItems() {
        // build list to be deleted
        final ArrayList<ItemWrapper> selectedItems = new ArrayList<ItemWrapper>();
        for (ItemWrapper item : mItems) {
            if (item.isChecked) {
                selectedItems.add(item);
            }
        }

        DialogFragment fragment = DeleteVideoDialogFragment.newInstance(selectedItems);
        fragment.show(getChildFragmentManager(), DeleteVideoDialogFragment.TAG);
    }

    public static class DeleteVideoDialogFragment extends DialogFragment {

        public static final String TAG = "DeleteVideoDialogFragment";

        private ArrayList<ItemWrapper> mItems;

        public static DeleteVideoDialogFragment newInstance(ArrayList<ItemWrapper> items) {
            DeleteVideoDialogFragment fragment = new DeleteVideoDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("items", items);
            fragment.setArguments(args);
            return fragment;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mItems = (ArrayList<ItemWrapper>) args.getSerializable("items");
            } else {
                mItems = (ArrayList<ItemWrapper>) savedInstanceState.getSerializable("items");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("items", mItems);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.video_play_title_delete_video)
                    .setMessage(R.string.video_play_msg_delete_video)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((VideoListFragment) getParentFragment()).doDelete(mItems);
                        }
                    })
                    .setNegativeButton(R.string.btn_no, null)
                    .create();
        }
    }

    private void doDelete(ArrayList<ItemWrapper> items) {
        for (ItemWrapper item : items) {
            if (item.file.isFile()) {
                File metadataFile = FileUtils.getMetadataFileFromVideoFile(item.file);
                metadataFile.delete();
				/*File oldMetadataFile = FileUtils.getOldMetadataFileFromVideoFile(getActivity(), item.file);
				oldMetadataFile.delete();*/
            }
            FileUtils.delete(item.file);
            mItems.remove(item);
        }
        mListAdapter.notifyDataSetChanged();

        if (mListAdapter.isEmpty()) {
            // up directory
            if (!mCurrDir.equals(mRootDir)) {
                mCurrDir = mCurrDir.getParentFile();
                mSelectedPosition = mPrevPosition;
                loadItems();
            } else {
                mMessageText.setText(R.string.video_play_msg_empty_video);
                mMessageText.setVisibility(View.VISIBLE);
            }
        } else {
            mMessageText.setVisibility(View.GONE);
        }
    }

    private void setShowVideo(boolean isShow) {
        ((VideoListActivity)getActivity()).setShowVideo(isShow);
    }

    private class ActionCallback implements ActionMode.Callback {

        private final boolean mShowArchiveMenu;
        private final boolean mShowShareMenu;

        public ActionCallback(boolean showArchiveMenu, boolean showShareMenu) {
            mShowArchiveMenu = showArchiveMenu;
            mShowShareMenu = showShareMenu;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_video_list, menu);
            if (!mShowArchiveMenu) {
                MenuItem item = menu.findItem(R.id.menu_archive);
                item.setVisible(false);
            }
            if (!mShowShareMenu) {
                MenuItem item = menu.findItem(R.id.menu_share);
                item.setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_select_all) {
                selectAllItems();
                return true;
            } else if (itemId == R.id.menu_unselect_all) {
                unselectAllItems();
                return true;
            } else if (itemId == R.id.menu_archive) {
                archiveSelectedItems();
                mode.finish();
                return true;
            } else if (itemId == R.id.menu_share) {
                shareSelectedItems();
                mode.finish();
                return true;
            } else if (itemId == R.id.menu_delete) {
                deleteSelectedItems();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;

            // NOTE: unselectAllItems method should be invoked in the next schedule
            mListView.post(new Runnable() {
                public void run() {
                    unselectAllItems();
                }
            });
        }

    }

    private static class ItemWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        private final File file;
        private final MediaMetadata metadata;
        private boolean isChecked;

        ItemWrapper(File dir, MediaMetadata metadata) {
            this.file = dir;
            this.metadata = metadata;
        }
    }

}
