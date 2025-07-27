package dngsoftware.acerfid;

import static androidx.core.app.ActivityCompat.requestPermissions;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@SuppressLint("GetInstance")
public class Utils {

    public static String[] materialWeights = {
            "1 KG",
            "750 G",
            "600 G",
            "500 G",
            "250 G"
    };

    public static int GetMaterialLength(String materialWeight) {
        switch (materialWeight) {
            case "1 KG":
                return 330;
            case "750 G":
                return 247;
            case "600 G":
                return 198;
            case "500 G":
                return 165;
            case "250 G":
                return 82;
        }
        return 330;
    }

    public static String GetMaterialWeight(int materialLength) {
        switch (materialLength) {
            case 330:
                return "1 KG";
            case 247:
                return "750 G";
            case 198:
                return "600 G";
            case 165:
                return "500 G";
            case 82:
                return "250 G";
        }
        return "1 KG";
    }

    public static void populateDatabase(MatDB db) {
        try {

            Filament filament = new Filament();
            filament.position =  0;
            filament.filamentID = "SHABBK-102";
            filament.filamentName = "ABS";
            filament.filamentVendor = "AC";
            filament.filamentParam = "220|250|90|100";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "";
            filament.filamentName = "ASA";
            filament.filamentVendor = "";
            filament.filamentParam = "240|280|90|100";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "";
            filament.filamentName = "PETG";
            filament.filamentVendor = "";
            filament.filamentParam = "230|250|70|90";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "AHPLBK-101";
            filament.filamentName = "PLA";
            filament.filamentVendor = "AC";
            filament.filamentParam = "190|230|50|60";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "AHPLPBK-102";
            filament.filamentName = "PLA+";
            filament.filamentVendor = "AC";
            filament.filamentParam = "210|230|45|60";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "";
            filament.filamentName = "PLA Glow";
            filament.filamentVendor = "";
            filament.filamentParam = "190|230|50|60";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "AHHSBK-103";
            filament.filamentName = "PLA High Speed";
            filament.filamentVendor = "AC";
            filament.filamentParam = "190|230|50|60";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "";
            filament.filamentName = "PLA Marble";
            filament.filamentVendor = "";
            filament.filamentParam = "200|230|50|60";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "HYGBK-102";
            filament.filamentName = "PLA Matte";
            filament.filamentVendor = "AC";
            filament.filamentParam = "190|230|55|65";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "";
            filament.filamentName = "PLA SE";
            filament.filamentVendor = "";
            filament.filamentParam = "190|230|55|65";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "AHSCWH-102";
            filament.filamentName = "PLA Silk";
            filament.filamentVendor = "AC";
            filament.filamentParam = "200|230|55|65";
            db.addItem(filament);

            filament = new Filament();
            filament.position = db.getItemCount();
            filament.filamentID = "STPBK-101";
            filament.filamentName = "TPU";
            filament.filamentVendor = "AC";
            filament.filamentParam = "210|230|25|60";
            db.addItem(filament);

        } catch (Exception ignored) {
        }
    }

    public static String[] getAllMaterials(MatDB db) {
        List<Filament> items = db.getAllItems();
        String[] materials = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            materials[i] = items.get(i).filamentName;
        }
        return materials;
    }

    public static int[] GetTemps(MatDB db, String materialName) {
        Filament item = db.getFilamentByName(materialName);
        String[] temps = item.filamentParam.split("\\|");
        int[] tempArray = new int[temps.length];
        for (int i = 0; i < temps.length; i++) {
            try {
                tempArray[i] = Integer.parseInt(temps[i].trim());
            } catch (Exception ignored) {
                return new int[]{200, 210, 50, 60};
            }
        }
        return tempArray;
    }

    public static byte[] GetSku(MatDB db, String materialName) {
        byte[] skuData = new byte[20];
        Arrays.fill(skuData, (byte) 0);
        Filament item = db.getFilamentByName(materialName);
        String sku = item.filamentID;
        if (sku != null && !sku.isEmpty()) {
            System.arraycopy(sku.getBytes(), 0, skuData, 0, sku.getBytes().length);
        }
        return skuData;
    }

    public static byte[] GetBrand(MatDB db, String materialName) {
        byte[] brandData = new byte[20];
        Arrays.fill(brandData, (byte) 0);
        Filament item = db.getFilamentByName(materialName);
        String brand = item.filamentVendor;
        if (brand != null && !brand.isEmpty()) {
            System.arraycopy(brand.getBytes(), 0, brandData, 0, brand.getBytes().length);
        }
        return brandData;
    }

    public static String bytesToHex(byte[] data, boolean space) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (space) {
                sb.append(String.format("%02X ", b));
            } else {
                sb.append(String.format("%02X", b));
            }
        }
        return sb.toString();
    }

    public static int getPixelColor(MotionEvent event, ImageView picker) {
        int viewX = (int) event.getX();
        int viewY = (int) event.getY();
        int viewWidth = picker.getWidth();
        int viewHeight = picker.getHeight();
        Bitmap image = ((BitmapDrawable) picker.getDrawable()).getBitmap();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int imageX = (int) ((float) viewX * ((float) imageWidth / (float) viewWidth));
        int imageY = (int) ((float) viewY * ((float) imageHeight / (float) viewHeight));
        return image.getPixel(imageX, imageY);
    }

    public static void SetPermissions(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
            String[] perms = {Manifest.permission.NFC};
            int permsRequestCode = 200;
            requestPermissions((Activity) context, perms, permsRequestCode);
        }
    }

    public static void playBeep() {
        new Thread(() -> {
            try {
                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50);
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300);
                toneGenerator.stopTone();
                toneGenerator.release();
            } catch (Exception ignored) {
            }
        }).start();
    }

    public static byte[] numToBytes(int value) {
        return revArray(new byte[]{(byte) (value >> 8), (byte) value});
    }

    public static int parseNumber(byte[] byteArray) {
        int result = 0;
        for (byte b : revArray(byteArray)) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    public static byte[] subArray(byte[] source, int startIndex, int length) {
        if (source == null) {
            return null;
        }
        int sourceLength = source.length;
        if (startIndex < 0 || startIndex >= sourceLength || length <= 0) {
            return new byte[0];
        }
        int endIndex = Math.min(startIndex + length, sourceLength);
        int actualLength = endIndex - startIndex;
        byte[] result = new byte[actualLength];
        System.arraycopy(source, startIndex, result, 0, actualLength);
        return result;
    }

    public static byte[] revArray(byte[] array) {
        if (array == null || array.length <= 1) {
            return array;
        }
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            byte temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    public static boolean arrayContains(String[] array, String string) {
        if (array == null || string == null) {
            return false;
        }
        for (String s : array) {
            if (s.contains(string.trim())) {
                return true;
            }
        }
        return false;
    }

    public static byte[] parseColor(final String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            try {
                byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                        + Character.digit(hexString.charAt(i + 1), 16));
            } catch (Exception e) {
                return new byte[]{(byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF};
            }
        }
        return revArray(rotateArray(byteArray));
    }

    public static byte[] rotateArray(byte[] b) {
        byte f = b[0];
        for (int i = 0; i < b.length - 1; i++) {
            b[i] = b[i + 1];
        }
        b[b.length - 1] = f;
        return b;
    }

    public static String parseColor(byte[] byteArray) {
        try {
            StringBuilder hexString = new StringBuilder();
            for (byte b : revArray(byteArray)) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return "FF0000FF";
        }
    }

    public static void openUrl(Context context, String url) {
        try {
            Uri webpage = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            context.startActivity(intent);
        } catch (Exception ignored) {}
    }

    public static float dp2Px(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static String GetSetting(Context context, String sKey, String sDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getString(sKey, sDefault);
    }

    public static boolean GetSetting(Context context, String sKey, boolean bDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getBoolean(sKey, bDefault);
    }

    public static int GetSetting(Context context, String sKey, int iDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getInt(sKey, iDefault);
    }

    public static long GetSetting(Context context, String sKey, long lDefault) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        return sharedPref.getLong(sKey, lDefault);
    }

    public static void SaveSetting(Context context, String sKey, String sValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(sKey, sValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, boolean bValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(sKey, bValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, int iValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(sKey, iValue);
        editor.apply();
    }

    public static void SaveSetting(Context context, String sKey, long lValue) {
        SharedPreferences sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(sKey, lValue);
        editor.apply();
    }

    public static void setVendorByItem(Spinner spinner, ArrayAdapter<String> adapter, String itemName) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (itemName.startsWith(Objects.requireNonNull(adapter.getItem(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    public static void setTypeByItem(Spinner spinner, ArrayAdapter<String> adapter, String itemName) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (itemName.contains(" " + Objects.requireNonNull(adapter.getItem(i)) + " ")) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    public static String[] filamentVendors = {
            "3Dgenius",
            "3DJake",
            "3DXTECH",
            "3D BEST-Q",
            "3D Hero",
            "3D-Fuel",
            "Aceaddity",
            "AddNorth",
            "Amazon Basics",
            "AMOLEN",
            "Ankermake",
            "Anycubic",
            "Atomic",
            "AzureFilm",
            "BASF",
            "Bblife",
            "BCN3D",
            "Beyond Plastic",
            "California Filament",
            "Capricorn",
            "CC3D",
            "colorFabb",
            "Comgrow",
            "Cookiecad",
            "Creality",
            "CERPRiSE",
            "Das Filament",
            "DO3D",
            "DOW",
            "DSM",
            "Duramic",
            "ELEGOO",
            "Eryone",
            "Essentium",
            "eSUN",
            "Extrudr",
            "Fiberforce",
            "Fiberlogy",
            "FilaCube",
            "Filamentive",
            "Fillamentum",
            "FLASHFORGE",
            "Formfutura",
            "Francofil",
            "FilamentOne",
            "Fil X",
            "GEEETECH",
            "Giantarm",
            "Gizmo Dorks",
            "GreenGate3D",
            "HATCHBOX",
            "Hello3D",
            "IC3D",
            "IEMAI",
            "IIID Max",
            "INLAND",
            "iProspect",
            "iSANMATE",
            "Justmaker",
            "Keene Village Plastics",
            "Kexcelled",
            "LDO",
            "MakerBot",
            "MatterHackers",
            "MIKA3D",
            "NinjaTek",
            "Nobufil",
            "Novamaker",
            "OVERTURE",
            "OVVNYXE",
            "Polymaker",
            "Priline",
            "Printed Solid",
            "Protopasta",
            "Prusament",
            "Push Plastic",
            "R3D",
            "Re-pet3D",
            "Recreus",
            "Regen",
            "Sain SMART",
            "SliceWorx",
            "Snapmaker",
            "SnoLabs",
            "Spectrum",
            "SUNLU",
            "TTYT3D",
            "Tianse",
            "UltiMaker",
            "Valment",
            "Verbatim",
            "VO3D",
            "Voxelab",
            "VOXELPLA",
            "YOOPAI",
            "Yousu",
            "Ziro",
            "Zyltech"};

    public static String[] filamentTypes = {
            "ABS",
            "ASA",
            "HIPS",
            "PA",
            "PA-CF",
            "PC",
            "PETG",
            "PLA",
            "PLA-CF",
            "PVA",
            "PP",
            "TPU"
    };

    public static int[] GetDefaultTemps(String materialType) {
        switch (materialType) {
            case "ABS":
                return new int[]{220, 250, 90, 100};
            case "ASA":
                return new int[]{240, 280, 90, 100};
            case "HIPS":
                return new int[]{230, 245, 80, 100};
            case "PA":
                return new int[]{220, 250, 70, 90};
            case "PA-CF":
                return new int[]{260, 280, 80, 100};
            case "PC":
                return new int[]{260, 300, 100, 110};
            case "PETG":
                return new int[]{230, 250, 70, 90};
            case "PLA":
                return new int[]{190, 230, 50, 60};
            case "PLA-CF":
                return new int[]{210, 240, 45, 65};
            case "PVA":
                return new int[]{215, 225, 45, 60};
            case "PP":
                return new int[]{225, 245, 80, 105};
            case "TPU":
                return new int[]{210, 230, 25, 60};
        }
        return new int[]{185, 300, 45, 110};
    }

    public static int[] presetColors() {
        return new int[]{
                Color.parseColor("#39C0CF"),
                Color.parseColor("#008080"),
                Color.parseColor("#0000CD"),
                Color.parseColor("#0059B3"),
                Color.parseColor("#00BFFF"),
                Color.parseColor("#87CEEB"),
                Color.parseColor("#FF8000"),
                Color.parseColor("#FFA500"),
                Color.parseColor("#FFCC00"),
                Color.parseColor("#B8860B"),
                Color.parseColor("#8B4513"),
                Color.parseColor("#A52A2A"),
                Color.parseColor("#FF0000"),
                Color.parseColor("#FF0033"),
                Color.parseColor("#FF4000"),
                Color.parseColor("#FF1493"),
                Color.parseColor("#C71585"),
                Color.parseColor("#800080"),
                Color.parseColor("#8B008B"),
                Color.parseColor("#CC99CC"),
                Color.parseColor("#008080"),
                Color.parseColor("#008080"),
                Color.parseColor("#40E0D0"),
                Color.parseColor("#008000"),
                Color.parseColor("#32CD32"),
                Color.parseColor("#7CFC00"),
                Color.parseColor("#36454F"),
                Color.parseColor("#778899"),
                Color.parseColor("#D3D3D3"),
                Color.parseColor("#696969"),
                Color.parseColor("#FFFF00"),
                Color.parseColor("#FFD700"),
                Color.parseColor("#F5DEB3"),
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#000000")
        };
    }

}