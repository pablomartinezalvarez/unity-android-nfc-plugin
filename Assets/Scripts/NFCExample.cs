using UnityEngine;
using System.Collections;

public class NFCExample : MonoBehaviour
{
	public GUIText nfc_output_text;
	AndroidJavaClass pluginTutorialActivityJavaClass;

	void Start ()
	{
		AndroidJNI.AttachCurrentThread ();
		pluginTutorialActivityJavaClass = new AndroidJavaClass ("com.twinsprite.nfcplugin.NFCPluginTest");
	}

	void Update ()
	{
		string value = pluginTutorialActivityJavaClass.CallStatic<string> ("getValue");
		nfc_output_text.text = "Value:\n" + value;
	}
}