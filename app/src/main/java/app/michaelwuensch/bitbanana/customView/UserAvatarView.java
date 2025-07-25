package app.michaelwuensch.bitbanana.customView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.constraintlayout.widget.ConstraintLayout;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnAddress;
import app.michaelwuensch.bitbanana.qrCodeGen.QRCodeGenerator;
import app.michaelwuensch.bitbanana.util.AvathorUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;

public class UserAvatarView extends ConstraintLayout implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ImageView mIvQRCode;
    private ImageFilterView mIvUserAvatar;
    private LightningNodeUri[] mNodeUris;
    private OnStateChangedListener mListener;
    private int mCurrentUriId = 0;
    private boolean mIsQRCodeIncluded;
    private String mCurrentAvatarCreationString;

    public UserAvatarView(Context context) {
        super(context);
        init();
    }

    public UserAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UserAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        View view = inflate(getContext(), R.layout.user_avatar_view, this);

        mIvQRCode = findViewById(R.id.qrCode);
        mIvUserAvatar = findViewById(R.id.userAvatar);

        showAvatar();
        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);
    }

    public void setupWithArbitraryString(String string, boolean includeQRCode) {
        reset();
        mCurrentAvatarCreationString = string;
        mIvUserAvatar.setImageBitmap(AvathorUtil.getAvathor(getContext(), mCurrentAvatarCreationString));
        showAvatar();
        mIsQRCodeIncluded = includeQRCode;
        if (mIsQRCodeIncluded) {
            // Generate "QR-Code"
            Bitmap bmpQRCode = QRCodeGenerator.bitmapFromText(string, 750);
            mIvQRCode.setImageBitmap(bmpQRCode);
            setupSwitchListeners();
        }
    }

    public void setupWithLNAddress(LnAddress lnAddress, boolean includeQRCode) {
        reset();
        mIsQRCodeIncluded = includeQRCode;
        if (mIsQRCodeIncluded) {
            setupSwitchListeners();
        }
        showAvatar();
        showIdentity(lnAddress);
    }

    public void setupWithNodeUri(LightningNodeUri nodeUri, boolean includeQRCode) {
        LightningNodeUri[] tempNodeUris = new LightningNodeUri[1];
        tempNodeUris[0] = nodeUri;

        setupWithNodeUris(tempNodeUris, includeQRCode);
    }

    public void setupWithNodeUris(LightningNodeUri[] nodeUris, boolean includeQRCode) {
        reset();
        mNodeUris = nodeUris;
        mIsQRCodeIncluded = includeQRCode;

        if (mIsQRCodeIncluded) {
            setupSwitchListeners();
        }

        showAvatar();
        showIdentity(true);
    }

    private void setupSwitchListeners() {

        mIvQRCode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAvatar();
                ((MotionLayout) findViewById(R.id.userAvatarMotionLayout)).transitionToStart();
                if (mListener != null) {
                    mListener.onHide();
                }
            }
        });

        mIvUserAvatar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showQRCode();
                ((MotionLayout) findViewById(R.id.userAvatarMotionLayout)).transitionToEnd();
                if (mListener != null) {
                    mListener.onReveal();
                }
            }
        });

    }

    public void showIdentity(boolean tor) {
        if (mNodeUris != null) {
            if (mNodeUris.length > 1) {
                for (int i = 0; i < mNodeUris.length; i++) {
                    if (mNodeUris[i].getHost() != null) {
                        if (mNodeUris[i].isTorUri() == tor) {
                            showIdentity(i);
                            return;
                        }
                    }
                }
            }
            showIdentity(0);
        }
    }

    private void showIdentity(LnAddress lnAddress) {
        if (mIsQRCodeIncluded) {
            // Generate "QR-Code"
            Bitmap bmpQRCode = QRCodeGenerator.bitmapFromText(lnAddress.toString(), 750);
            mIvQRCode.setImageBitmap(bmpQRCode);
        }
        // Load user Avatar
        mCurrentAvatarCreationString = lnAddress.toString();
        mIvUserAvatar.setImageBitmap(AvathorUtil.getAvathor(getContext(), mCurrentAvatarCreationString));
    }

    private void showIdentity(int id) {
        if (mNodeUris != null) {
            mCurrentUriId = Math.min(mNodeUris.length, id);

            if (mNodeUris[mCurrentUriId] != null) {
                if (mIsQRCodeIncluded) {
                    // Generate "QR-Code"
                    Bitmap bmpQRCode = QRCodeGenerator.bitmapFromText(mNodeUris[mCurrentUriId].getAsString(), 750);
                    mIvQRCode.setImageBitmap(bmpQRCode);
                }

                // Load user Avatar
                mCurrentAvatarCreationString = mNodeUris[mCurrentUriId].getPubKey();
                mIvUserAvatar.setImageBitmap(AvathorUtil.getAvathor(getContext(), mCurrentAvatarCreationString));
            }
        }
    }

    public void reset() {
        mNodeUris = null;
        mCurrentAvatarCreationString = null;
        mIsQRCodeIncluded = false;
        mCurrentUriId = 0;
        mIvUserAvatar.setOnClickListener(null);
        mIvQRCode.setOnClickListener(null);
        mIvUserAvatar.setImageResource(R.drawable.unknown_avatar);
    }

    private void showQRCode() {
        mIvQRCode.setElevation(2);
        mIvUserAvatar.setElevation(1);
        mIvQRCode.setKeepScreenOn(true);
    }

    private void showAvatar() {
        mIvQRCode.setElevation(1);
        mIvUserAvatar.setElevation(2);
        mIvQRCode.setKeepScreenOn(false);
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            // Update the image
            if (key.equals(PrefsUtil.AVATAR_STYLE) && mCurrentAvatarCreationString != null) {
                mIvUserAvatar.setImageBitmap(AvathorUtil.getAvathor(getContext(), mCurrentAvatarCreationString));
            }
        }
    }

    public interface OnStateChangedListener {
        void onReveal();

        void onHide();
    }

    public LightningNodeUri getCurrentNodeIdentity() {
        if (mNodeUris == null) {
            return null;
        } else {
            return mNodeUris[mCurrentUriId];
        }
    }

    public boolean isCurrentNodeIdentityTor() {
        if (mNodeUris == null) {
            return false;
        } else {
            if (mNodeUris[mCurrentUriId].getHost() != null) {
                return mNodeUris[mCurrentUriId].isTorUri();
            } else {
                return false;
            }
        }
    }

    public boolean hasTorAndPublicIdentity() {
        if (mNodeUris == null) {
            return false;
        }
        if (mNodeUris.length > 1) {
            boolean hasPublic = false;
            boolean hasTor = false;
            for (LightningNodeUri nodeUri : mNodeUris) {
                if (nodeUri.getHost() != null && nodeUri.isTorUri()) {
                    hasTor = true;
                } else if (nodeUri.getHost() == null || !nodeUri.isTorUri()) {
                    hasPublic = true;
                }
            }
            return hasPublic && hasTor;
        } else {
            return false;
        }
    }
}
