package zlc.season.rxdownloadproject.basic_download;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import zlc.season.rxdownload.DownloadRecord;
import zlc.season.rxdownload.DownloadStatus;
import zlc.season.rxdownload.RxDownload;
import zlc.season.rxdownloadproject.DownloadStateContext;
import zlc.season.rxdownloadproject.R;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class BasicDownloadActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.img)
    ImageView mImg;
    @BindView(R.id.status)
    TextView mStatus;
    @BindView(R.id.percent)
    TextView mPercent;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.size)
    TextView mSize;
    @BindView(R.id.action)
    Button mAction;
    @BindView(R.id.finish)
    Button mFinish;

    private String saveName = "weixin.apk";
    private String url = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";
    private Subscription subscription;
    private DownloadStateContext mStateContext;

    @OnClick({R.id.action, R.id.finish})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.action:
                mStateContext.performClick(new DownloadStateContext.Callback() {
                    @Override
                    public void startDownload() {
                        start();
                    }

                    @Override
                    public void cancelDownload() {

                    }

                    @Override
                    public void pauseDownload() {
                        pause();
                    }

                    @Override
                    public void install() {
                        installApk();
                    }
                });
                break;
            case R.id.finish:
                BasicDownloadActivity.this.finish();
                break;
        }
    }

    void unSubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        String icon = "http://static.yingyonghui.com/icon/128/4200197.png";
        Picasso.with(this).load(icon).into(mImg);
        mStatus.setText("开始");

        mStateContext = new DownloadStateContext(mStatus, mAction);
        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unSubscribe(subscription);
    }

    private void start() {
        subscription = RxPermissions.getInstance(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .observeOn(Schedulers.io())
                .compose(RxDownload.getInstance().transform(url, saveName, null))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_COMPLETED);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", e);
                        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_FAILED);
                    }

                    @Override
                    public void onNext(final DownloadStatus status) {
                        mProgress.setIndeterminate(status.isChunked);
                        mProgress.setMax((int) status.getTotalSize());
                        mProgress.setProgress((int) status.getDownloadSize());
                        mPercent.setText(status.getPercent());
                        mSize.setText(status.getFormatStatusString());
                    }
                });
    }

    private void pause() {
        BasicDownloadActivity.this.unSubscribe(subscription);
        mStateContext.setStateAndDisplay(DownloadRecord.FLAG_PAUSED);
    }

    private void installApk() {
        Uri uri = Uri.fromFile(new File(getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath() +
                File.separator + saveName));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }
}