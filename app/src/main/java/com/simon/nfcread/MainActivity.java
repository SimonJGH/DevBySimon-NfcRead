package com.simon.nfcread;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

@SuppressWarnings("all")
public class MainActivity extends AppCompatActivity {
    private EditText mNoteRead;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private String NFC_ID = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNoteRead = ((EditText) findViewById(R.id.noteRead));

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);// 设备注册
        if (mNfcAdapter == null) {// 判断设备是否可用
            Toast.makeText(this, "设备不支持nfc!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "请在系统设置中先启用NFC功能！", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            finish();
            return;
        }

        mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // 读取uidgetIntent()
        byte[] myNFCID = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        NFC_ID = Converter.getHexString(myNFCID, myNFCID.length);
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundNdefPush(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, null, null);// ++
        resolvIntent(getIntent());
    }

    void setUpWebView(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0)
            return;
        for (int i = 0; i < msgs.length; i++) {
            int lenth = msgs[i].getRecords().length;
            NdefRecord[] records = msgs[i].getRecords();
            for (int j = 0; j < lenth; j++) {
                for (NdefRecord record : records) {
                    short tnf = record.getTnf();
                    if (tnf == NdefRecord.TNF_WELL_KNOWN) {
                        parseWellKnownTextRecode(record);
                    }
                }
            }
        }
    }

    private void parseWellKnownTextRecode(NdefRecord record) {
        Preconditions.checkArgument(Arrays.equals(record.getType(),
                NdefRecord.RTD_TEXT));
        String payloadStr = "";
        byte[] payload = record.getPayload();
        Byte statusByte = record.getPayload()[0];
        String textEncoding = "";
        textEncoding = ((statusByte & 0200) == 0) ? "utf-8" : "utf-16";
        int languageCodeLength = 0;
        languageCodeLength = statusByte & 0077;
        try {
            payloadStr = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
        }
        setNoteBody(payloadStr);
    }

    private void setNoteBody(String body) {
        Editable text = mNoteRead.getText();
        text.clear();
        text.append("CONTENT:" + body + "\n" + "NFC_ID:" + NFC_ID);
    }

    void resolvIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,
                        empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
            setUpWebView(msgs);
        }
    }
}
