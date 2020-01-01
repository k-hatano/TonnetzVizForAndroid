//package org.wisterina.android.tool.dialogfragment.fileselect;
package jp.nita.utils;

import java.io.Serializable;


/**
 * ファイル選択のリスナーです。
 */
public interface OnFileSelectedListener extends Serializable {

    /**
     * ファイルが選択された時に呼び出されます。
     * @param path パス
     */
    public abstract void onFileSelected(String path);
    /**
     * ファイル選択がキャンセルされたときに呼び出されます。
     */
    public abstract void onFileSelectCanceled();
}