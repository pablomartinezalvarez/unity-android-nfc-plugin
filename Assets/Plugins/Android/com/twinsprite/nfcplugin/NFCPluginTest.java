package com.twinsprite.nfcplugin;

import java.nio.charset.Charset;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;
import com.unity3d.player.UnityPlayerActivity;

public class NFCPluginTest extends UnityPlayerActivity {

	public static final String MIME_TEXT_PLAIN = "text/plain";

	private static final BiMap URI_PREFIX_MAP = ImmutableBiMap.builder().put((byte) 0x00, "")
			.put((byte) 0x01, "http://www.").put((byte) 0x02, "https://www.").put((byte) 0x03, "http://")
			.put((byte) 0x04, "https://").put((byte) 0x05, "tel:").put((byte) 0x06, "mailto:")
			.put((byte) 0x07, "ftp://anonymous:anonymous@").put((byte) 0x08, "ftp://ftp.").put((byte) 0x09, "ftps://")
			.put((byte) 0x0A, "sftp://").put((byte) 0x0B, "smb://").put((byte) 0x0C, "nfs://")
			.put((byte) 0x0D, "ftp://").put((byte) 0x0E, "dav://").put((byte) 0x0F, "news:")
			.put((byte) 0x10, "telnet://").put((byte) 0x11, "imap:").put((byte) 0x12, "rtsp://")
			.put((byte) 0x13, "urn:").put((byte) 0x14, "pop:").put((byte) 0x15, "sip:").put((byte) 0x16, "sips:")
			.put((byte) 0x17, "tftp:").put((byte) 0x18, "btspp://").put((byte) 0x19, "btl2cap://")
			.put((byte) 0x1A, "btgoep://").put((byte) 0x1B, "tcpobex://").put((byte) 0x1C, "irdaobex://")
			.put((byte) 0x1D, "file://").put((byte) 0x1E, "urn:epc:id:").put((byte) 0x1F, "urn:epc:tag:")
			.put((byte) 0x20, "urn:epc:pat:").put((byte) 0x21, "urn:epc:raw:").put((byte) 0x22, "urn:epc:")
			.put((byte) 0x23, "urn:nfc:").build();

	private NfcAdapter mNfcAdapter;

	private PendingIntent pendingIntent;

	private IntentFilter[] mIntentFilter;

	private static String value = "";

	private String[][] techListsArray;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Foreground Dispatch: 1. Creates a PendingIntent object so the Android
		// system can populate it with the details of the tag when it is
		// scanned.
		pendingIntent = PendingIntent.getActivity(NFCPluginTest.this, 0,
				new Intent(NFCPluginTest.this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Foreground Dispatch: 2. Declare intent filters to handle the intents
		// that you want to intercept
		mIntentFilter = new IntentFilter[] { new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };

		// Foreground Dispatch: 3. Set up an array of tag technologies that your
		// application wants to handle.
		techListsArray = new String[][] { new String[] { NfcF.class.getName() } };

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Log.e(NFCPluginTest.class.toString(), "This device doesn't support NFC.");
			finish();
			return;
		}

		if (!mNfcAdapter.isEnabled()) {
			Log.e(NFCPluginTest.class.toString(), "NFC is disabled.");
		} else {
			Log.i(NFCPluginTest.class.toString(), "NFC reader initialized.");
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mNfcAdapter.enableForegroundDispatch(this, pendingIntent, mIntentFilter, techListsArray);
	}

	@Override
	public void onPause() {
		super.onPause();
		mNfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {

		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (tag != null) {

			// parse through all NDEF messages and their records and pick text
			// type only
			Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

			String s = "";

			if (data != null) {
				try {
					for (int i = 0; i < data.length; i++) {
						NdefRecord[] recs = ((NdefMessage) data[i]).getRecords();
						for (int j = 0; j < recs.length; j++) {
							if (recs[j].getTnf() == NdefRecord.TNF_WELL_KNOWN
									&& Arrays.equals(recs[j].getType(), NdefRecord.RTD_TEXT)) {
								/*
								 * See NFC forum specification for
								 * "Text Record Type Definition" at 3.2.1
								 * 
								 * http://www.nfc-forum.org/specs/
								 * 
								 * bit_7 defines encoding bit_6 reserved for
								 * future use, must be 0 bit_5..0 length of IANA
								 * language code
								 */
								byte[] payload = recs[j].getPayload();
								String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
								int langCodeLen = payload[0] & 0077;
								s += new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1,
										textEncoding);
							} else if (recs[j].getTnf() == NdefRecord.TNF_WELL_KNOWN
									&& Arrays.equals(recs[j].getType(), NdefRecord.RTD_URI)) {
								/*
								 * See NFC forum specification for
								 * "URI Record Type Definition" at 3.2.2
								 * 
								 * http://www.nfc-forum.org/specs/
								 * 
								 * payload[0] contains the URI Identifier Code
								 * payload[1]...payload[payload.length - 1]
								 * contains the rest of the URI.
								 */
								byte[] payload = recs[j].getPayload();
								String prefix = (String) URI_PREFIX_MAP.get(payload[0]);
								byte[] fullUri = Bytes.concat(prefix.getBytes(Charset.forName("UTF-8")),
										Arrays.copyOfRange(payload, 1, payload.length));
								s += new String(fullUri, Charset.forName("UTF-8"));
							}
						}
					}
				} catch (Exception e) {
					value = e.getMessage();
					Log.e(NFCPluginTest.class.toString(), e.getMessage());
				}
			}
			Log.i(NFCPluginTest.class.toString(), s);
			value = s;
		}
	}

	public static String getValue() {
		return value;
	}
}