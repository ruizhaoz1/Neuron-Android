package org.nervos.neuron.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.nervos.neuron.R;
import org.nervos.neuron.activity.AboutUsActivity;
import org.nervos.neuron.activity.CurrencyActivity;
import org.nervos.neuron.activity.SimpleWebActivity;
import org.nervos.neuron.activity.WalletManageActivity;
import org.nervos.neuron.item.WalletItem;
import org.nervos.neuron.service.http.EthRpcService;
import org.nervos.neuron.util.Blockies;
import org.nervos.neuron.util.db.DBWalletUtil;
import org.nervos.neuron.util.url.HttpUrls;
import org.nervos.neuron.view.SettingButtonView;
import org.nervos.neuron.view.dialog.AuthFingerDialog;
import org.nervos.neuron.util.ConstantUtil;
import org.nervos.neuron.util.fingerprint.AuthenticateResultCallback;
import org.nervos.neuron.util.fingerprint.FingerPrintController;
import org.nervos.neuron.util.db.SharePrefUtil;
import org.nervos.neuron.view.dialog.SelectorDialog;
import org.nervos.neuron.view.dialog.ToastSingleButtonDialog;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by duanyytop on 2018/4/17
 */
public class SettingsFragment extends NBaseFragment {

    private static final int Currency_Code = 10001;
    public static final String TAG = SettingsFragment.class.getName();
    private SettingButtonView mSbvCurrency, mSbvAboutUs, mSbvContactUs, mSbvFingerPrint, mSbvForums, mSbvSelectEth;
    private TextView walletNameText, walletAddressText;
    private CircleImageView photoImage;
    private RelativeLayout walletLayout;
    private AuthFingerDialog mAuthFingerDialog = null;
    private SelectorDialog mEthNodeDialog = null;
    private SparseArray<String> ethNodeList = new SparseArray<>();
    private int ethNodeIndex;
    private EthNodeAdapter mEthNodeAdapter;
    private FingerPrintController mFingerPrintController;

    @Override
    protected int getContentLayout() {
        return R.layout.fragment_settings;
    }

    @Override
    public void initView() {
        mSbvCurrency = (SettingButtonView) findViewById(R.id.sbv_local_coin);
        mSbvAboutUs = (SettingButtonView) findViewById(R.id.sbv_about_us);
        mSbvContactUs = (SettingButtonView) findViewById(R.id.sbv_contact_us);
        mSbvFingerPrint = (SettingButtonView) findViewById(R.id.sbv_fingerprint);
        mSbvForums = (SettingButtonView) findViewById(R.id.sbv_forums);
        mSbvSelectEth = (SettingButtonView) findViewById(R.id.sbv_eth_select);
        walletNameText = (TextView) findViewById(R.id.text_wallet_name);
        walletAddressText = (TextView) findViewById(R.id.text_wallet_address);
        walletLayout = (RelativeLayout) findViewById(R.id.wallet_information_layout);
        photoImage = (CircleImageView) findViewById(R.id.wallet_photo);
    }

    @Override
    public void initData() {
        WalletItem walletItem = DBWalletUtil.getCurrentWallet(getContext());
        walletNameText.setText(walletItem.name);
        walletAddressText.setText(walletItem.address);
        photoImage.setImageBitmap(Blockies.createIcon(walletItem.address));

        initEthNode();

        mFingerPrintController = new FingerPrintController(getActivity());
        mSbvCurrency.setRightText(SharePrefUtil.getString(ConstantUtil.CURRENCY, ConstantUtil.DEFAULT_CURRENCY));
        if (mFingerPrintController.isSupportFingerprint()) {
            mSbvFingerPrint.setVisibility(View.VISIBLE);
            if (SharePrefUtil.getBoolean(ConstantUtil.FINGERPRINT, false)) {
                mSbvFingerPrint.setSwitchCheck(true);
            } else {
                SharePrefUtil.putBoolean(ConstantUtil.FINGERPRINT, false);
                mSbvFingerPrint.setSwitchCheck(false);
            }
        } else {
            mSbvFingerPrint.setVisibility(View.GONE);
        }
        String node = SharePrefUtil.getString(ConstantUtil.ETH_NET, ConstantUtil.ETH_MAINNET);
        if (node.contains("_")) node = node.replace("_", " ");
        mSbvSelectEth.setRightText(node);
    }

    @Override
    public void initAction() {
        mSbvCurrency.setOnClickListener(() -> {
            Intent intent = new Intent(getActivity(), CurrencyActivity.class);
            startActivityForResult(intent, Currency_Code);
        });
        mSbvFingerPrint.setSwitchCheckedListener(new SettingButtonView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isChecked) {
                if (isChecked) {
                    //setting fingerprint
                    if (mFingerPrintController.hasEnrolledFingerprints() && mFingerPrintController.getEnrolledFingerprints().size() > 0) {
                        if (mAuthFingerDialog == null) mAuthFingerDialog = new AuthFingerDialog(getActivity());
                        mAuthFingerDialog.setOnShowListener((dialogInterface) -> {
                            mFingerPrintController.authenticate(authenticateResultCallback);
                        });
                        mAuthFingerDialog.setOnDismissListener((dialog) -> {
                            mFingerPrintController.cancelAuth();
                            mSbvFingerPrint.setSwitchCheck(false);
                        });
                        mAuthFingerDialog.show();
                    } else {
                        ToastSingleButtonDialog dialog = ToastSingleButtonDialog.getInstance(getActivity(), getResources().getString(R.string.dialog_finger_setting));
                        dialog.setOnCancelClickListener(view -> {
                            FingerPrintController.openFingerPrintSettingPage(getActivity());
                            view.dismiss();
                        });
                    }
                } else {
                    //close fingerprint
                    SharePrefUtil.putBoolean(ConstantUtil.FINGERPRINT, false);
                    mSbvFingerPrint.setSwitchCheck(false);
                }
            }
        });
        mSbvAboutUs.setOnClickListener(() -> {
            Intent intent = new Intent(getActivity(), AboutUsActivity.class);
            startActivity(intent);
        });
        mSbvContactUs.setOnClickListener(() -> {
            ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData mClipData = ClipData.newPlainText("contact", "Nervos-Neuron");
            if (cm != null) {
                cm.setPrimaryClip(mClipData);
                Toast.makeText(getActivity(), R.string.copy_weixin_success, Toast.LENGTH_SHORT).show();
            }
        });
        mSbvForums.setOnClickListener(() -> {
            SimpleWebActivity.gotoSimpleWeb(getActivity(), HttpUrls.NERVOS_TALK_URL);
        });
        mSbvSelectEth.setOnClickListener(() -> {
            mEthNodeDialog = new SelectorDialog(getActivity());
            mEthNodeDialog.setTitleText(getString(R.string.setting_select_eth_net));
            mEthNodeDialog.setRecyclerView(mEthNodeAdapter);
            mEthNodeDialog.setOkListener((view -> {
                ethNodeIndex = mEthNodeAdapter.mIndex;
                SharePrefUtil.putString(ConstantUtil.ETH_NET, ethNodeList.get(ethNodeIndex));
                mEthNodeDialog.dismiss();
            }));
            mEthNodeDialog.setOnDissmissListener(dialogInterface -> {
                EthRpcService.initNodeUrl();
                String node = SharePrefUtil.getString(ConstantUtil.ETH_NET, ConstantUtil.ETH_MAINNET);
                if (node.contains("_")) node = node.replace("_", " ");
                mSbvSelectEth.setRightText(node);
                mEthNodeDialog.dismiss();
            });
        });
        walletLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), WalletManageActivity.class));
            }
        });
    }

    private void initEthNode() {
        ethNodeList.put(0, ConstantUtil.ETH_MAINNET);
        ethNodeList.put(1, ConstantUtil.ETH_NET_ROPSTEN_TEST);
        ethNodeList.put(2, ConstantUtil.ETH_NET_KOVAN_TEST);
        ethNodeList.put(3, ConstantUtil.ETH_NET_RINKEBY_TEST);
        switch (SharePrefUtil.getString(ConstantUtil.ETH_NET, ConstantUtil.ETH_NET_MAIN)) {
            case ConstantUtil.ETH_NET_MAIN:
            default:
                ethNodeIndex = 0;
                break;
            case ConstantUtil.ETH_NET_ROPSTEN_TEST:
                ethNodeIndex = 1;
                break;
            case ConstantUtil.ETH_NET_KOVAN_TEST:
                ethNodeIndex = 2;
                break;
            case ConstantUtil.ETH_NET_RINKEBY_TEST:
                ethNodeIndex = 3;
                break;
        }
        mEthNodeAdapter = new EthNodeAdapter(ethNodeIndex);
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
    }

    AuthenticateResultCallback authenticateResultCallback = new AuthenticateResultCallback() {
        @Override
        public void onAuthenticationError(String errorMsg) {
            Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationSucceeded() {
            mSbvFingerPrint.setSwitchCheck(true);
            if (mAuthFingerDialog != null && mAuthFingerDialog.isShowing()) mAuthFingerDialog.dismiss();
            SharePrefUtil.putBoolean(ConstantUtil.FINGERPRINT, true);
            Toast.makeText(getContext(), getResources().getString(R.string.fingerprint_setting_sucess), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationFailed() {
            Toast.makeText(getContext(), getResources().getString(R.string.fingerprint_setting_failed), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Currency_Code:
                    mSbvCurrency.setRightText(SharePrefUtil.getString(ConstantUtil.CURRENCY, "CNY"));
                    break;
            }
        }
    }

    class EthNodeAdapter extends RecyclerView.Adapter<ViewHolder> {

        public int mIndex;

        public EthNodeAdapter(int index) {
            mIndex = index;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_currency, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (mIndex == position) holder.mSbvEthNode.setRightImageShow(true);
            else holder.mSbvEthNode.setRightImageShow(false);

            holder.mSbvEthNode.setNameText(ethNodeList.get(position).replace("_", " "));
            holder.mSbvEthNode.setOnClickListener(() -> {
                mIndex = holder.getAdapterPosition();
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return ethNodeList.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        SettingButtonView mSbvEthNode;

        public ViewHolder(View view) {
            super(view);
            mSbvEthNode = view.findViewById(R.id.currency);
        }
    }
}
