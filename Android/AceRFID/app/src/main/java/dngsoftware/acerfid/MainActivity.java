package dngsoftware.acerfid;

import static android.view.View.TEXT_ALIGNMENT_CENTER;
import dngsoftware.acerfid.databinding.ActivityMainBinding;
import dngsoftware.acerfid.databinding.AddDialogBinding;
import dngsoftware.acerfid.databinding.PickerDialogBinding;
import static dngsoftware.acerfid.Utils.GetBrand;
import static dngsoftware.acerfid.Utils.GetDefaultTemps;
import static dngsoftware.acerfid.Utils.GetMaterialLength;
import static dngsoftware.acerfid.Utils.GetMaterialWeight;
import static dngsoftware.acerfid.Utils.GetSku;
import static dngsoftware.acerfid.Utils.GetTemps;
import static dngsoftware.acerfid.Utils.SetPermissions;
import static dngsoftware.acerfid.Utils.arrayContains;
import static dngsoftware.acerfid.Utils.bytesToHex;
import static dngsoftware.acerfid.Utils.filamentTypes;
import static dngsoftware.acerfid.Utils.filamentVendors;
import static dngsoftware.acerfid.Utils.getAllMaterials;
import static dngsoftware.acerfid.Utils.materialWeights;
import static dngsoftware.acerfid.Utils.numToBytes;
import static dngsoftware.acerfid.Utils.GetSetting;
import static dngsoftware.acerfid.Utils.SaveSetting;
import static dngsoftware.acerfid.Utils.openUrl;
import static dngsoftware.acerfid.Utils.parseColor;
import static dngsoftware.acerfid.Utils.parseNumber;
import static dngsoftware.acerfid.Utils.playBeep;
import static dngsoftware.acerfid.Utils.populateDatabase;
import static dngsoftware.acerfid.Utils.presetColors;
import static dngsoftware.acerfid.Utils.rotateArray;
import static dngsoftware.acerfid.Utils.setTypeByItem;
import static dngsoftware.acerfid.Utils.setVendorByItem;
import static dngsoftware.acerfid.Utils.subArray;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.room.Room;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private MatDB matDb;
    private NfcAdapter nfcAdapter;
    Tag currentTag = null;
    ArrayAdapter<String> madapter, sadapter;
    String MaterialName, MaterialWeight = "1 KG", MaterialColor = "FF0000FF";
    Dialog pickerDialog, addDialog;
    AlertDialog inputDialog;
    int SelectedSize;
    boolean userSelect = false;
    private ActivityMainBinding main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getApplicationContext().getResources();
        Locale locale = new Locale("en");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());

        main = ActivityMainBinding.inflate(getLayoutInflater());
        View rv = main.getRoot();
        setContentView(rv);

        main.editbutton.setVisibility(View.INVISIBLE);
        main.deletebutton.setVisibility(View.INVISIBLE);

        main.colorview.setOnClickListener(view -> openPicker());
        main.colorview.setBackgroundColor(Color.argb(255, 0, 0, 255));
        main.readbutton.setOnClickListener(view -> readTag(currentTag));
        main.writebutton.setOnClickListener(view -> writeTag(currentTag));

        main.addbutton.setOnClickListener(view -> openAddDialog(false));
        main.editbutton.setOnClickListener(view -> openAddDialog(true));

        main.deletebutton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.delete_filament);
            builder.setMessage(MaterialName);
            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                if (matDb.getFilamentByName(MaterialName) != null) {
                    matDb.deleteItem(matDb.getFilamentByName(MaterialName));
                    loadMaterials(false);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog alert = builder.create();
            alert.show();
        });

        filamentDB rdb = Room.databaseBuilder(this, filamentDB.class, "filament_database")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        matDb = rdb.matDB();

        if (matDb.getItemCount() == 0) {
            populateDatabase(matDb);
        }

        SetPermissions(this);

        try {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                Bundle options = new Bundle();
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
                nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A, options);
            } else {
                Toast.makeText(getApplicationContext(), R.string.please_activate_nfc, Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                finish();
            }
        } catch (Exception ignored) {
        }

        sadapter = new ArrayAdapter<>(this, R.layout.spinner_item, materialWeights);
        main.spoolsize.setAdapter(sadapter);
        main.spoolsize.setSelection(SelectedSize);
        main.spoolsize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SelectedSize = main.spoolsize.getSelectedItemPosition();
                MaterialWeight = sadapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        loadMaterials(false);

        main.colorspin.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    openPicker();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    break;
            }
            return false;
        });

        main.autoread.setChecked(GetSetting(this, "autoread", false));
        main.autoread.setOnCheckedChangeListener((buttonView, isChecked) -> SaveSetting(this, "autoread", isChecked));

        ReadTagUID(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            try {
                nfcAdapter.disableReaderMode(this);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (pickerDialog != null && pickerDialog.isShowing()) {
            pickerDialog.dismiss();
            openPicker();
        }
        if (inputDialog != null && inputDialog.isShowing()) {
            inputDialog.dismiss();
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            currentTag = tag;
            runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), getString(R.string.tag_found) + bytesToHex(currentTag.getId(), false), Toast.LENGTH_SHORT).show();
                main.tagid.setText(bytesToHex(currentTag.getId(), true));
                if (GetSetting(this, "autoread", false)) {
                    readTag(currentTag);
                }
            });
        } catch (Exception ignored) {
        }
    }

    void loadMaterials(boolean select)
    {
        madapter = new ArrayAdapter<>(this, R.layout.spinner_item, getAllMaterials(matDb));
        main.material.setAdapter(madapter);
        main.material.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                MaterialName = madapter.getItem(position);
                assert MaterialName != null;
                main.infotext.setText(String.format(Locale.getDefault(), getString(R.string.info_temps),
                        GetTemps(matDb, MaterialName)[0], GetTemps(matDb, MaterialName)[1], GetTemps(matDb, MaterialName)[2], GetTemps(matDb, MaterialName)[3]));

                if (position <= 11){
                    main.editbutton.setVisibility(View.INVISIBLE);
                    main.deletebutton.setVisibility(View.INVISIBLE);
                }else {
                    main.editbutton.setVisibility(View.VISIBLE);
                    main.deletebutton.setVisibility(View.VISIBLE);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        if (select) {
            main.material.setSelection(madapter.getPosition(MaterialName));
        }
        else {
            main.material.setSelection(3);
        }
    }

    void ReadTagUID(Intent intent) {
        if (intent != null) {
            try {
                if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                    currentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    assert currentTag != null;
                    Toast.makeText(getApplicationContext(), getString(R.string.tag_found) + bytesToHex(currentTag.getId(), false), Toast.LENGTH_SHORT).show();
                    main.tagid.setText(bytesToHex(currentTag.getId(), true));
                    if (GetSetting(this, "autoread", false)) {
                        readTag(currentTag);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void readTag(Tag tag) {
        if (tag == null) {
            Toast.makeText(getApplicationContext(), R.string.no_nfc_tag_found, Toast.LENGTH_SHORT).show();
            return;
        }
        NfcA nfcA = NfcA.get(tag);
        if (nfcA != null) {
            try {
                nfcA.connect();
                byte[] data = new byte[144];
                ByteBuffer buff = ByteBuffer.wrap(data);
                for (int page = 4; page <= 36; page += 4) {
                    byte[] pageData = nfcA.transceive(new byte[] {(byte) 0x30, (byte)page});
                    if (pageData != null) {
                        buff.put(pageData);
                    }
                }
                if (buff.array()[0] != (byte) 0x00) {
                    userSelect = true;
                    MaterialName = new String(subArray(buff.array(), 44, 16), StandardCharsets.UTF_8).trim();
                    main.material.setSelection(madapter.getPosition(MaterialName));
                    MaterialColor = parseColor(rotateArray(subArray(buff.array(), 64, 4)));
                    main.colorview.setBackgroundColor(Color.parseColor("#" + MaterialColor));
                    // String sku = new String(subArray(buff.array(), 4, 16), StandardCharsets.UTF_8 ).trim();
                    // String Brand = new String(subArray(buff.array(), 24, 16), StandardCharsets.UTF_8).trim();
                    int extMin = parseNumber(subArray(buff.array(), 80, 2));
                    int extMax = parseNumber(subArray(buff.array(), 82, 2));
                    int bedMin = parseNumber(subArray(buff.array(), 100, 2));
                    int bedMax = parseNumber(subArray(buff.array(), 102, 2));
                    main.infotext.setText(String.format(Locale.getDefault(), getString(R.string.info_temps), extMin, extMax, bedMin, bedMax));
                    // int diameter = parseNumber(subArray(buff.array(),104,2));
                    MaterialWeight = GetMaterialWeight(parseNumber(subArray(buff.array(), 106, 2)));
                    main.spoolsize.setSelection(sadapter.getPosition(MaterialWeight));
                    Toast.makeText(getApplicationContext(), R.string.data_read_from_tag, Toast.LENGTH_SHORT).show();
                    userSelect = false;
                } else {
                    Toast.makeText(getApplicationContext(), R.string.unknown_or_empty_tag, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ignored) {
                Toast.makeText(getApplicationContext(), R.string.error_reading_tag, Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    nfcA.close();
                } catch (Exception ignored) {
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.invalid_tag_type, Toast.LENGTH_SHORT).show();
        }
    }

    private void nfcAWritePage(NfcA nfcA, int page, byte[] data) throws Exception {
        byte[] cmd = new byte[6];
        cmd[0] = (byte) 0xA2;
        cmd[1] = (byte) page;
        System.arraycopy(data, 0, cmd, 2, data.length);
        nfcA.transceive(cmd);
    }

    private void writeTag(Tag tag) {
        if (tag == null) {
            Toast.makeText(getApplicationContext(), R.string.no_nfc_tag_found, Toast.LENGTH_SHORT).show();
            return;
        }
        NfcA nfcA = NfcA.get(tag);
        if (nfcA != null) {
            try {
                nfcA.connect();
                nfcAWritePage(nfcA, 4, new byte[]{123, 0, 101, 0});
                for (int i = 0; i < 5; i++) { //sku
                    nfcAWritePage(nfcA, 5 + i, subArray(GetSku(matDb, MaterialName), i * 4, 4));
                }
                for (int i = 0; i < 5; i++) { //brand
                    nfcAWritePage(nfcA, 10 + i, subArray(GetBrand(matDb, MaterialName), i * 4, 4));
                }
                byte[] matData = new byte[20];
                Arrays.fill(matData, (byte) 0);
                System.arraycopy(MaterialName.getBytes(), 0, matData, 0, Math.min(20, MaterialName.length()));
                nfcAWritePage(nfcA, 15, subArray(matData, 0, 4));   //type
                nfcAWritePage(nfcA, 16, subArray(matData, 4, 4));   //type
                nfcAWritePage(nfcA, 17, subArray(matData, 8, 4));   //type
                nfcAWritePage(nfcA, 18, subArray(matData, 12, 4));  //type
                nfcAWritePage(nfcA, 20, parseColor(MaterialColor)); //color
                //ultralight.writePage(23, new byte[] {50, 0, 100, 0});   //more temps?
                byte[] extTemp = new byte[4];
                System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[0]), 0, extTemp, 0, 2); //min
                System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[1]), 0, extTemp, 2, 2); //max
                nfcAWritePage(nfcA, 24, extTemp);
                byte[] bedTemp = new byte[4];
                System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[2]), 0, bedTemp, 0, 2); //min
                System.arraycopy(numToBytes(GetTemps(matDb, MaterialName)[3]), 0, bedTemp, 2, 2); //max
                nfcAWritePage(nfcA, 29, bedTemp);
                byte[] filData = new byte[4];
                System.arraycopy(numToBytes(175), 0, filData, 0, 2); //diameter
                System.arraycopy(numToBytes(GetMaterialLength(MaterialWeight)), 0, filData, 2, 2); //length
                nfcAWritePage(nfcA, 30, filData);
                nfcAWritePage(nfcA, 31, new byte[]{(byte) 232, 3, 0, 0}); //?
                playBeep();
                Toast.makeText(getApplicationContext(), R.string.data_written_to_tag, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
                Toast.makeText(getApplicationContext(), R.string.error_writing_to_tag, Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    nfcA.close();
                } catch (Exception ignored) {
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.invalid_tag_type, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    void openPicker() {
        try {
            pickerDialog = new Dialog(this, R.style.Theme_AceRFID);
            pickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            pickerDialog.setCanceledOnTouchOutside(false);
            pickerDialog.setTitle(R.string.pick_color);
            PickerDialogBinding dl = PickerDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            pickerDialog.setContentView(rv);

            dl.btncls.setOnClickListener(v -> {
                if (dl.txtcolor.getText().toString().length() == 8) {
                    try {
                        MaterialColor = dl.txtcolor.getText().toString();
                        int color = Color.argb(dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                        main.colorview.setBackgroundColor(color);
                    } catch (Exception ignored) {
                    }
                }
                pickerDialog.dismiss();
            });

            dl.redSlider.setProgress(Color.red(Color.parseColor("#" + MaterialColor)));
            dl.greenSlider.setProgress(Color.green(Color.parseColor("#" + MaterialColor)));
            dl.blueSlider.setProgress(Color.blue(Color.parseColor("#" + MaterialColor)));
            dl.alphaSlider.setProgress(Color.alpha(Color.parseColor("#" + MaterialColor)));

            setupPresetColors(dl);
            updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());

            SeekBar.OnSeekBarChangeListener rgbChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            };

            dl.redSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.greenSlider.setOnSeekBarChangeListener(rgbChangeListener);
            dl.blueSlider.setOnSeekBarChangeListener(rgbChangeListener);

            dl.alphaSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    updateColorDisplay(dl, dl.alphaSlider.getProgress(), dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            dl.txtcolor.setOnClickListener(v -> showHexInputDialog(dl));

            pickerDialog.show();
        } catch (Exception ignored) {
        }
    }

    void openAddDialog(boolean edit) {
        try {

            if (!Utils.GetSetting(this,"CFN",false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.notice);
                builder.setMessage(R.string.cf_notice);
                builder.setPositiveButton(R.string.accept, (dialog, which) -> {
                    Utils.SaveSetting(this, "CFN", true);
                    dialog.dismiss();
                    openAddDialog(edit);
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog alert = builder.create();
                alert.show();
                return;
            }

            addDialog = new Dialog(this, R.style.Theme_AceRFID);
            addDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            addDialog.setCanceledOnTouchOutside(false);
            addDialog.setTitle(R.string.add_filament);
            AddDialogBinding dl = AddDialogBinding.inflate(getLayoutInflater());
            View rv = dl.getRoot();
            addDialog.setContentView(rv);
            dl.btncls.setOnClickListener(v -> addDialog.dismiss());
            dl.btnhlp.setOnClickListener(v -> openUrl(this,getString(R.string.helpurl)));

            dl.chkvendor.setOnClickListener(v -> {
                if (dl.chkvendor.isChecked()) {
                    dl.vendor.setVisibility(View.INVISIBLE);
                    dl.txtvendor.setVisibility(View.VISIBLE);

                } else {
                    dl.vendor.setVisibility(View.VISIBLE);
                    dl.txtvendor.setVisibility(View.INVISIBLE);

                }
            });

            if (edit) {
                dl.btnsave.setText(R.string.save);
                dl.lbltitle.setText(R.string.edit_filament);
            }
            else {
                dl.btnsave.setText(R.string.add);
                dl.lbltitle.setText(R.string.add_filament);
            }

           dl.btnsave.setOnClickListener(v -> {
               if (dl.txtserial.getText().toString().isEmpty() || dl.txtextmin.getText().toString().isEmpty() || dl.txtextmax.getText().toString().isEmpty() || dl.txtbedmin.getText().toString().isEmpty() || dl.txtbedmax.getText().toString().isEmpty()) {
                   Toast.makeText(getApplicationContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                   return;
               }
               if (dl.chkvendor.isChecked() && dl.txtvendor.getText().toString().isEmpty()) {
                   Toast.makeText(getApplicationContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                   return;
               }

               String vendor = dl.vendor.getSelectedItem().toString();
               if (dl.chkvendor.isChecked())
               {
                   vendor = dl.txtvendor.getText().toString().trim();
               }
               if (edit) {
                   updateFilament(vendor, dl.type.getSelectedItem().toString(), dl.txtserial.getText().toString(), dl.txtextmin.getText().toString(), dl.txtextmax.getText().toString(), dl.txtbedmin.getText().toString(), dl.txtbedmax.getText().toString());
               } else {
                   addFilament(vendor, dl.type.getSelectedItem().toString(), dl.txtserial.getText().toString(), dl.txtextmin.getText().toString(), dl.txtextmax.getText().toString(), dl.txtbedmin.getText().toString(), dl.txtbedmax.getText().toString());
               }

               addDialog.dismiss();
           });

            ArrayAdapter<String> vadapter = new ArrayAdapter<>(this, R.layout.spinner_item, filamentVendors);
            dl.vendor.setAdapter(vadapter);

            ArrayAdapter<String> tadapter = new ArrayAdapter<>(this, R.layout.spinner_item, filamentTypes);
            dl.type.setAdapter(tadapter);

            dl.type.setOnTouchListener((v, event) -> {
                userSelect = true;
                v.performClick();
                return false;
            });

            dl.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (userSelect) {
                        String tmpType = Objects.requireNonNull(tadapter.getItem(position));
                        int[] temps = GetDefaultTemps(tmpType);
                        dl.txtextmin.setText(String.valueOf(temps[0]));
                        dl.txtextmax.setText(String.valueOf(temps[1]));
                        dl.txtbedmin.setText(String.valueOf(temps[2]));
                        dl.txtbedmax.setText(String.valueOf(temps[3]));
                        userSelect = false;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    userSelect = false;
                }
            });

            if (edit) {
                setTypeByItem(dl.type, tadapter, MaterialName);
                try {
                    if (!arrayContains(filamentVendors, MaterialName.split(dl.type.getSelectedItem().toString() + " ")[0].trim())) {
                        dl.chkvendor.setChecked(true);
                        dl.txtvendor.setVisibility(View.VISIBLE);
                        dl.vendor.setVisibility(View.INVISIBLE);
                        dl.txtvendor.setText(MaterialName.split(dl.type.getSelectedItem().toString() + " ")[0].trim());
                    } else {
                        dl.chkvendor.setChecked(false);
                        dl.txtvendor.setVisibility(View.INVISIBLE);
                        dl.vendor.setVisibility(View.VISIBLE);
                        setVendorByItem(dl.vendor, vadapter, MaterialName);
                    }
                } catch (Exception ignored) {
                    dl.chkvendor.setChecked(false);
                    dl.txtvendor.setVisibility(View.INVISIBLE);
                    dl.vendor.setVisibility(View.VISIBLE);
                    dl.vendor.setSelection(0);
                }
                try {
                    dl.txtserial.setText(MaterialName.split(dl.type.getSelectedItem().toString() + " ")[1]);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    dl.txtserial.setText("");
                }
                int[] temps = GetTemps(matDb, MaterialName);
                dl.txtextmin.setText(String.valueOf(temps[0]));
                dl.txtextmax.setText(String.valueOf(temps[1]));
                dl.txtbedmin.setText(String.valueOf(temps[2]));
                dl.txtbedmax.setText(String.valueOf(temps[3]));

            }else {
                dl.vendor.setSelection(0);
                dl.type.setSelection(7);
                int[] temps = GetDefaultTemps("PLA");
                dl.txtextmin.setText(String.valueOf(temps[0]));
                dl.txtextmax.setText(String.valueOf(temps[1]));
                dl.txtbedmin.setText(String.valueOf(temps[2]));
                dl.txtbedmax.setText(String.valueOf(temps[3]));
            }

            addDialog.show();
        } catch (Exception ignored) {}
    }

    void addFilament(String tmpVendor, String tmpType, String tmpSerial, String tmpExtMin, String tmpExtMax, String tmpBedMin, String tmpBedMax) {
        Filament filament = new Filament();
        filament.position =  matDb.getItemCount();
        filament.filamentID = "";
        filament.filamentName = String.format("%s %s %s", tmpVendor.trim(), tmpType, tmpSerial.trim());
        filament.filamentVendor = "";
        filament.filamentParam = String.format("%s|%s|%s|%s", tmpExtMin, tmpExtMax, tmpBedMin, tmpBedMax);
        matDb.addItem(filament);
        loadMaterials(false);
    }

    void updateFilament(String tmpVendor, String tmpType, String tmpSerial, String tmpExtMin, String tmpExtMax, String tmpBedMin, String tmpBedMax) {
        Filament currentFilament = matDb.getFilamentByName(MaterialName);
        int tmpPosition = currentFilament.position;
        matDb.deleteItem(currentFilament);
        MaterialName = String.format("%s %s %s", tmpVendor.trim(), tmpType, tmpSerial.trim());
        Filament filament = new Filament();
        filament.position =  tmpPosition;
        filament.filamentID = "";
        filament.filamentName = MaterialName;
        filament.filamentVendor = "";
        filament.filamentParam = String.format("%s|%s|%s|%s", tmpExtMin, tmpExtMax, tmpBedMin, tmpBedMax);
        matDb.addItem(filament);
        loadMaterials(true);
    }

    private void updateColorDisplay(PickerDialogBinding dl, int currentAlpha,int currentRed,int currentGreen,int currentBlue) {
        int color = Color.argb(currentAlpha, currentRed, currentGreen, currentBlue);
        dl.colorDisplay.setBackgroundColor(color);
        String hexCode = rgbToHexA(currentRed, currentGreen, currentBlue, currentAlpha);
        dl.txtcolor.setText(hexCode);
        double alphaNormalized = currentAlpha / 255.0;
        int blendedRed = (int) (currentRed * alphaNormalized + 244 * (1 - alphaNormalized));
        int blendedGreen = (int) (currentGreen * alphaNormalized + 244 * (1 - alphaNormalized));
        int blendedBlue = (int) (currentBlue * alphaNormalized + 244 * (1 - alphaNormalized));
        double brightness = (0.299 * blendedRed + 0.587 * blendedGreen + 0.114 * blendedBlue) / 255;
        if (brightness > 0.5) {
            dl.txtcolor.setTextColor(Color.BLACK);
        } else {
            dl.txtcolor.setTextColor(Color.WHITE);
        }

    }

    private String rgbToHexA(int r, int g, int b, int a) {
        return String.format("%02X%02X%02X%02X", a, r, g, b);
    }

    private void setupPresetColors(PickerDialogBinding dl) {
        for (int color : presetColors()) {
            Button colorButton = new Button(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = (int) getResources().getDimension(R.dimen.preset_circle_size);
            params.height = (int) getResources().getDimension(R.dimen.preset_circle_size);
            params.setMargins(
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin),
                    (int) getResources().getDimension(R.dimen.preset_circle_margin)
            );
            colorButton.setLayoutParams(params);
            GradientDrawable circleDrawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(),R.drawable.circle_shape,null);
            assert circleDrawable != null;
            circleDrawable.setColor(color);
            colorButton.setBackground(circleDrawable);
            colorButton.setTag(color);

            colorButton.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                setSlidersFromColor(dl, selectedColor);
            });
            dl.presetColorGrid.addView(colorButton);
        }
    }

    private void setSlidersFromColor(PickerDialogBinding dl, int argbColor) {
        dl.redSlider.setProgress(Color.red(argbColor));
        dl.greenSlider.setProgress(Color.green(argbColor));
        dl.blueSlider.setProgress(Color.blue(argbColor));
        dl.alphaSlider.setProgress(Color.alpha(argbColor));
        updateColorDisplay(dl, Color.alpha(argbColor), Color.red(argbColor), Color.green(argbColor), Color.blue(argbColor));
    }

    private void showHexInputDialog(PickerDialogBinding dl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.enter_hex_color_aarrggbb);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint(R.string.aarrggbb);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        input.setText(rgbToHexA(dl.redSlider.getProgress(), dl.greenSlider.getProgress(), dl.blueSlider.getProgress(), dl.alphaSlider.getProgress()));
        InputFilter[] filters = new InputFilter[3];
        filters[0] = new HexInputFilter();
        filters[1] = new InputFilter.LengthFilter(8);
        filters[2] = new InputFilter.AllCaps();
        input.setFilters(filters);
        builder.setView(input);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.submit, (dialog, which) -> {
            String hexInput = input.getText().toString().trim();
            if (isValidHexCode(hexInput)) {
                setSlidersFromColor(dl, Color.parseColor("#" + hexInput));
            } else {
                Toast.makeText(MainActivity.this, R.string.invalid_hex_code_please_use_aarrggbb_format, Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        inputDialog = builder.create();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidthPx = displayMetrics.widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int maxWidthDp = 100;
        int maxWidthPx = (int) (maxWidthDp * density);
        int dialogWidthPx = (int) (screenWidthPx * 0.80);
        if (dialogWidthPx > maxWidthPx) {
            dialogWidthPx = maxWidthPx;
        }
        Objects.requireNonNull(inputDialog.getWindow()).setLayout(dialogWidthPx, WindowManager.LayoutParams.WRAP_CONTENT);
        inputDialog.getWindow().setGravity(Gravity.CENTER); // Center the dialog on the screen
        inputDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = inputDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = inputDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(Color.parseColor("#82B1FF"));
            negativeButton.setTextColor(Color.parseColor("#82B1FF"));
        });
        inputDialog.show();
    }

    private static class HexInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (Character.isDigit(character) || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F')) {
                    filtered.append(character);
                }
            }
            return filtered.toString();
        }
    }

    private boolean isValidHexCode(String hexCode) {
        Pattern pattern = Pattern.compile("^[0-9a-fA-F]{8}$");
        Matcher matcher = pattern.matcher(hexCode);
        return matcher.matches();
    }
}