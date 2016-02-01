/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.hippo.beerbelly.LruMap;
import com.hippo.conaco.Conaco;
import com.hippo.drawable.ImageWrapper;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.network.StatusCodeException;
import com.hippo.okhttp.CookieDB;
import com.hippo.scene.SceneApplication;
import com.hippo.utils.ReadableTime;
import com.hippo.yorozuya.IntIdGenerator;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class EhApplication extends SceneApplication {

    private IntIdGenerator mIdGenerator = new IntIdGenerator();
    private SparseArray<Object> mGlobalStuffMap = new SparseArray<>();
    private EhCookieStore mEhCookieStore;
    private EhClient mEhClient;
    private OkHttpClient mOkHttpClient;
    private ImageWrapperHelper mImageWrapperHelper;
    private Conaco<ImageWrapper> mConaco;
    private LruMap<Integer, GalleryDetail> mGalleryDetailCache;

    @Override
    public void onCreate() {
        super.onCreate();

        GetText.initialize(this);
        CookieDB.initialize(this);
        StatusCodeException.initialize(this);
        Settings.initialize(this);
        ReadableTime.initialize(this);
    }

    public int putGlobalStuff(@NonNull Object o) {
        int id = mIdGenerator.nextId();
        mGlobalStuffMap.put(id, o);
        return id;
    }

    public boolean containGlobalStuff(int id) {
        return mGlobalStuffMap.indexOfKey(id) >= 0;
    }

    public Object getGlobalStuff(int id) {
        return mGlobalStuffMap.get(id);
    }

    public Object removeGlobalStuff(int id) {
        int index = mGlobalStuffMap.indexOfKey(id);
        if (index >= 0) {
            Object o = mGlobalStuffMap.valueAt(index);
            mGlobalStuffMap.removeAt(index);
            return o;
        } else {
            return null;
        }
    }

    public boolean removeGlobalStuff(Object o) {
        int index = mGlobalStuffMap.indexOfValue(o);
        if (index >= 0) {
            mGlobalStuffMap.removeAt(index);
            return true;
        } else {
            return false;
        }
    }

    public static EhCookieStore getEhCookieStore(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhCookieStore == null) {
            application.mEhCookieStore = new EhCookieStore();
        }
        return application.mEhCookieStore;
    }

    @NonNull
    public static EhClient getEhClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mEhClient == null) {
            application.mEhClient = new EhClient(application);
        }
        return application.mEhClient;
    }

    @NonNull
    public static OkHttpClient getOkHttpClient(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mOkHttpClient == null) {
            application.mOkHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .cookieJar(getEhCookieStore(application))
                    .build();
        }
        return application.mOkHttpClient;
    }

    @NonNull
    public static ImageWrapperHelper getImageWrapperHelper(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mImageWrapperHelper == null) {
            application.mImageWrapperHelper = new ImageWrapperHelper();
        }
        return application.mImageWrapperHelper;
    }

    private static int getMemoryCacheMaxSize(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.
                getSystemService(Context.ACTIVITY_SERVICE);
        return Math.min(20 * 1024 * 1024,
                Math.round(0.2f * activityManager.getMemoryClass() * 1024 * 1024));
    }

    @NonNull
    public static Conaco<ImageWrapper> getConaco(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mConaco == null) {
            Conaco.Builder<ImageWrapper> builder = new Conaco.Builder<>();
            builder.hasMemoryCache = true;
            builder.memoryCacheMaxSize = getMemoryCacheMaxSize(context);
            builder.hasDiskCache = true;
            builder.diskCacheDir = new File(context.getCacheDir(), "thumb");
            builder.diskCacheMaxSize = 80 * 1024 * 1024; // 80MB
            builder.okHttpClient = getOkHttpClient(context);
            builder.objectHelper = getImageWrapperHelper(context);
            builder.debug = BuildConfig.DEBUG;
            application.mConaco = builder.build();
        }
        return application.mConaco;
    }

    @NonNull
    public static LruMap<Integer, GalleryDetail> getGalleryDetailCache(@NonNull Context context) {
        EhApplication application = ((EhApplication) context.getApplicationContext());
        if (application.mGalleryDetailCache == null) {
            application.mGalleryDetailCache = new LruMap<>(new Comparator<Integer>() {
                @Override
                public int compare(Integer lhs, Integer rhs) {
                    return lhs - rhs;
                }
            }, 3 * 60 * 1000); // 3 min timeout
        }
        return application.mGalleryDetailCache;
    }
}
