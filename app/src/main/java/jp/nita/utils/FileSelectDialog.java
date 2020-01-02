/*
 * Copyright 2017 Nobuki HIRAMINE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// package com.hiramine.fileselectiondialogtest;
package jp.nita.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSelectDialog implements AdapterView.OnItemClickListener {
    static public class FileInfo implements Comparable<FileInfo> {
        private String m_strName;
        private File m_file;

        public FileInfo(String strName, File file) {
            m_strName = strName;
            m_file = file;
        }

        public String getName() {
            return m_strName;
        }

        public File getFile() {
            return m_file;
        }

        public int compareTo(FileInfo another) {
            if (m_file.isDirectory() && !another.getFile().isDirectory()) {
                return -1;
            }
            if (!m_file.isDirectory() && another.getFile().isDirectory()) {
                return 1;
            }

            return m_file.getName().toLowerCase().compareTo(another.getFile().getName().toLowerCase());
        }
    }

    static public class FileInfoArrayAdapter extends BaseAdapter {
        private Context m_context;
        private List<FileInfo> m_listFileInfo;

        public FileInfoArrayAdapter(Context context, List<FileInfo> list) {
            super();
            m_context = context;
            m_listFileInfo = list;
        }

        @Override
        public int getCount() {
            return m_listFileInfo.size();
        }

        @Override
        public FileInfo getItem(int position) {
            return m_listFileInfo.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        static class ViewHolder {
            TextView tvFileName;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (null == convertView) {
                LinearLayout layout = new LinearLayout(m_context);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView tvFileName = new TextView(m_context);
                tvFileName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                tvFileName.setPadding(8, 8, 8, 8);
                layout.addView(tvFileName, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                convertView = layout;
                viewHolder = new ViewHolder();
                viewHolder.tvFileName = tvFileName;
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            FileInfo fileinfo = m_listFileInfo.get(position);
            if (fileinfo.getFile().isDirectory()) {
                viewHolder.tvFileName.setText(fileinfo.getName() + "/");
            } else {
                viewHolder.tvFileName.setText(fileinfo.getName());
            }

            return convertView;
        }
    }

    private Context m_contextParent;
    private OnFileSelectListener m_listener;
    private AlertDialog m_dialog;
    private FileInfoArrayAdapter m_fileInfoArrayAdapter;
    private String[] m_extensions;

    public FileSelectDialog(Context context, OnFileSelectListener listener) {
        m_contextParent = context;
        m_listener = listener;
    }

    public void show(File fileDirectory, String[] extensions) {
        m_extensions = extensions;
        String strTitle = fileDirectory.getAbsolutePath();

        ListView listview = new ListView(m_contextParent);
        listview.setScrollingCacheEnabled(false);
        listview.setOnItemClickListener(this);

        File[] aFile = fileDirectory.listFiles();
        List<FileInfo> listFileInfo = new ArrayList<>();
        if (null != aFile) {
            for (File fileTemp : aFile) {
                if (fileTemp.isFile() && extensions != null && extensions.length > 0) {
                    boolean isRelevantExtension = false;
                    for (String extension : extensions) {
                        if (fileTemp.getName().toLowerCase().endsWith(extension)) {
                            isRelevantExtension = true;
                            break;
                        }
                    }
                    if (isRelevantExtension == false) {
                        continue;
                    }
                }
                listFileInfo.add(new FileInfo(fileTemp.getName(), fileTemp));
            }
            Collections.sort(listFileInfo);
        }
        if (null != fileDirectory.getParent()) {
            listFileInfo.add(0, new FileInfo("..", new File(fileDirectory.getParent())));
        }
        m_fileInfoArrayAdapter = new FileInfoArrayAdapter(m_contextParent, listFileInfo);
        listview.setAdapter(m_fileInfoArrayAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(m_contextParent);
        builder.setTitle(strTitle);
        builder.setNegativeButton("Cancel", null);
        builder.setView(listview);
        m_dialog = builder.show();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != m_dialog) {
            m_dialog.dismiss();
            m_dialog = null;
        }

        FileInfo fileinfo = m_fileInfoArrayAdapter.getItem(position);

        if (fileinfo.getFile().isDirectory()) {
            show(fileinfo.getFile(), m_extensions);
        } else {
            m_listener.onFileSelect(fileinfo.getFile());
        }
    }

    public interface OnFileSelectListener {
        void onFileSelect(File file);
    }
}


