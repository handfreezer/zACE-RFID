using ACE_RFID.Properties;
using PCSC;
using PCSC.Monitoring;
using System;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using static ACE_RFID.Utils;

namespace ACE_RFID
{
    public partial class MainForm : Form
    {

        private ISCardContext context = null;
        private Monitor monitor;
        private ICardReader icReader;
        private Reader reader;
        private string materialColor, FwVersion;
        private readonly System.Windows.Forms.ToolTip toolTip = new System.Windows.Forms.ToolTip(), balloonTip = new System.Windows.Forms.ToolTip();

        public MainForm()
        {
            InitializeComponent();
        }

        private void CardInserted(CardStatusEventArgs args)
        {
            try
            {
                icReader = context.ConnectReader(args.ReaderName, SCardShareMode.Shared, SCardProtocol.Any);
                reader = new Reader(icReader);
                byte[] uid = reader.GetData();
                Invoke((MethodInvoker)delegate ()
                {
                    if (uid == null)
                    {
                        Toast.Show(this, "Tag not compatible", Toast.LENGTH_LONG, true);
                        icReader.Dispose();
                        reader = null;
                        return;
                    }
                    if (string.IsNullOrEmpty(FwVersion))
                    {
                        FwVersion = ReaderVersion(reader);
                        Text = string.IsNullOrEmpty(FwVersion) ? "CFS RFID" : FwVersion;
                    }
                    lblUid.Text = BitConverter.ToString(uid).Replace("-", " ");
                    lblTagId.Visible = true;
                    balloonTip.SetToolTip(lblUid, "UID: " + lblUid.Text);
                    if (chkAutoRead.Checked)
                    {
                        ReadTag();
                    }
                    else if (chkAutoWrite.Checked) 
                    { 
                        WriteTag(); 
                    }
                });
            }
            catch { }
        }

        private void CardRemoved(CardStatusEventArgs args)
        {
            try
            {
                if (icReader != null)
                {
                    icReader.Dispose();
                    reader = null;
                }
                Invoke((MethodInvoker)delegate ()
                {
                    lblUid.Text = string.Empty;
                    lblTagId.Visible = false;
                });
            }
            catch { }
        }

        private void ConnectReader()
        {
            try
            {
                lblConnect.Visible = false;
                if (context == null)
                {
                    context = ContextFactory.Instance.Establish(SCardScope.System);
                }
                var readers = context.GetReaders();
                if (readers.Length > 0)
                {
                    monitor?.Dispose();
                    monitor = new Monitor(readers);
                    monitor.CardInserted += CardInserted;
                    monitor.CardRemoved += CardRemoved;
                    lblConnect.Visible = false;
                }
                else
                {
                    lblConnect.Visible = true;
                    Toast.Show(this, "Connect Failed", Toast.LENGTH_SHORT);
                }
            }
            catch (Exception e)
            {
                lblConnect.Visible = true;
                Toast.Show(this, e.Message, Toast.LENGTH_LONG, true);
            }
        }

        private void MainForm_Load(object sender, EventArgs e)
        {

            BackColor = ColorTranslator.FromHtml("#F4F4F4");
            lblConnect.BackColor = ColorTranslator.FromHtml("#F4F4F4");
            btnRead.BackColor = ColorTranslator.FromHtml("#1976D2");
            btnWrite.BackColor = ColorTranslator.FromHtml("#1976D2");
            btnSave.BackColor = ColorTranslator.FromHtml("#1976D2");
            btnCls.BackColor = ColorTranslator.FromHtml("#1976D2");

            btnDel.Visible = false;
            btnEdit.Visible = false;

            panel1.Location = new Point(0, 0);
            lblConnect.Location = new Point(0, 0);

            if (MatDB.GetItemCount() == 0)
            {
                MatDB.PopulateDatabase();
            }

            materialType.Items.AddRange(GetAllMaterials());
            cboType.Items.AddRange(filamentTypes);
            cboType.AutoCompleteMode = AutoCompleteMode.SuggestAppend;
            cboType.AutoCompleteSource = AutoCompleteSource.ListItems;
            cboType.DropDownStyle = ComboBoxStyle.DropDown;
            cboVendor.Items.AddRange(filamentVendors);
            cboVendor.AutoCompleteMode = AutoCompleteMode.SuggestAppend;
            cboVendor.AutoCompleteSource = AutoCompleteSource.ListItems;
            cboVendor.DropDownStyle = ComboBoxStyle.DropDown;

            materialType.Text = "PLA";
            materialWeight.Text = "1 KG";
            materialColor = "0000FF";
            btnColor.BackColor = ColorTranslator.FromHtml("#" + materialColor);

            btnRead.FlatAppearance.BorderSize = 0;
            btnWrite.FlatAppearance.BorderSize = 0;
            btnColor.FlatAppearance.BorderSize = 0;
            btnSave.FlatAppearance.BorderSize = 0;
            btnCls.FlatAppearance.BorderSize = 0;
            btnDel.FlatAppearance.BorderSize = 0;
            btnEdit.FlatAppearance.BorderSize = 0;
            btnAdd.FlatAppearance.BorderSize = 0;

            toolTip.SetToolTip(btnDel, "Delete selected filament");
            toolTip.SetToolTip(btnEdit, "Edit selected filament");
            toolTip.SetToolTip(btnAdd, "Add a new filament");
            balloonTip.IsBalloon = true;

            ConnectReader();
        }

        private void MainForm_FormClosed(object sender, FormClosedEventArgs e)
        {
            Environment.Exit(0);
        }

        void WriteTag()
        {
            try
            {
                if (reader != null && icReader.IsConnected)
                {

                    byte[] buffer = new byte[144];

                    new byte[] { 123, 0, 101, 0 }.CopyTo(buffer, 0);

                    GetSku(materialType.Text).CopyTo(buffer, 4); //sku

                    GetBrand(materialType.Text).CopyTo(buffer, 24); //brand

                    Encoding.UTF8.GetBytes(materialType.Text).CopyTo(buffer, 44); //type

                    buffer[64] = (byte)0xFF;
                    if (materialColor.Equals("000000")) { materialColor = "010101"; } // basic fix for anycubic setting black to transparent
                    ParseColor(materialColor).CopyTo(buffer, 65); //color

                    NumToBytes(GetTemps(materialType.Text)[0]).CopyTo(buffer, 80); //ext min
                    NumToBytes(GetTemps(materialType.Text)[1]).CopyTo(buffer, 82); //ext max
                    NumToBytes(GetTemps(materialType.Text)[2]).CopyTo(buffer, 100); //bed min
                    NumToBytes(GetTemps(materialType.Text)[3]).CopyTo(buffer, 102); //bed max

                    NumToBytes(175).CopyTo(buffer, 104); //diameter
                    NumToBytes(GetMaterialLength(materialWeight.Text)).CopyTo(buffer, 106); //length


                    reader.WriteData(buffer);

                    Toast.Show(this, "Data written to TAG");
                }
                else
                {
                    Toast.Show(this, "Tag not found");
                }
            }
            catch (Exception)
            {
                Toast.Show(this, "Error writing to TAG");
            }
        }

        private void ReadTag()
        {
            try
            {
                if (reader != null && icReader.IsConnected)
                {
                    byte[] buffer = reader.ReadData();

                    // String sku = Encoding.UTF8.GetString(SubArray(buffer, 4, 20)).Trim();
                    // String Brand = Encoding.UTF8.GetString(SubArray(buffer, 24, 20)).Trim();

                    materialType.Text = Encoding.UTF8.GetString(SubArray(buffer, 44, 20)).Trim();
                    materialColor = ParseColor(SubArray(buffer, 65, 3));
                    if (materialColor.Equals("010101")) { materialColor = "000000"; } // basic fix for anycubic setting black to transparent
                    btnColor.BackColor = ColorTranslator.FromHtml("#" + materialColor);

                    int extMin = ParseNumber(SubArray(buffer, 80, 2));
                    int extMax = ParseNumber(SubArray(buffer, 82, 2));
                    int bedMin = ParseNumber(SubArray(buffer, 100, 2));
                    int bedMax = ParseNumber(SubArray(buffer, 102, 2));

                    lblTemps.Text = String.Format(Resources.tempMessage, extMin, extMax, bedMin, bedMax);


                    // int diameter = ParseNumber(SubArray(buffer,104,2));
                    materialWeight.Text = GetMaterialWeight(ParseNumber(SubArray(buffer, 106, 2)));

                    Toast.Show(this, "Data read from TAG");
                }
                else
                {
                    Toast.Show(this, "Tag not found");
                }
            }
            catch (Exception)
            {
                Toast.Show(this, "Error reading TAG");
            }

        }

        private void BtnRead_Click(object sender, EventArgs e)
        {
            ReadTag();
        }

        private void BtnWrite_Click(object sender, EventArgs e)
        {
            WriteTag();
        }

        private void BtnColor_Click(object sender, EventArgs e)
        {
            if (colorDialog1.ShowDialog() == DialogResult.OK)
            {
                btnColor.BackColor = colorDialog1.Color;
                materialColor = (colorDialog1.Color.ToArgb() & 0x00FFFFFF).ToString("X6");
            }
        }

        private void LblConnect_Click(object sender, EventArgs e)
        {
            ConnectReader();
        }

        private void BtnAdd_Click(object sender, EventArgs e)
        {
            if (GetSetting("CFN", false) == false)
            {
                DialogResult result = MessageBox.Show(Resources.customMessage, "Notice", MessageBoxButtons.OKCancel, MessageBoxIcon.Question);
                if (result == DialogResult.OK)
                {
                    SaveSetting("CFN", true);
                }
                else
                {
                    return;
                }
            }

            cboType.Text = cboType.Items[0].ToString();
            cboVendor.Text = cboVendor.Items[0].ToString();
            btnSave.Text = "Add";
            panel1.Visible = true;
        }

        private void BtnEdit_Click(object sender, EventArgs e)
        {
            if (GetSetting("CFN", false) == false)
            {
                DialogResult result = MessageBox.Show(Resources.customMessage, "Notice", MessageBoxButtons.OKCancel, MessageBoxIcon.Question);
                if (result == DialogResult.OK)
                {
                    SaveSetting("CFN", true);
                }
                else
                {
                    return;
                }
            }

            Filament filament = MatDB.GetFilamentByName(materialType.Text);
            int pos = filament.Position;
            if (pos > 11)
            {
                SetTypeByItem(cboType, filament.FilamentName);

                string filamentVendor = filament.FilamentName.Split(new string[] { " " + cboType.Text + " " }, StringSplitOptions.None)[0].Trim();

                if (ArrayContains(filamentVendors, filamentVendor))
                {
                    cboVendor.Text = filamentVendor;
                }

                txtSerial.Text = filament.FilamentName.Split(new string[] { " " + cboType.Text + " " }, StringSplitOptions.None)[1].Trim();
                txtExtMin.Text = filament.FilamentParam.Split('|')[0];
                txtExtMax.Text = filament.FilamentParam.Split('|')[1];
                txtBedMin.Text = filament.FilamentParam.Split('|')[2];
                txtBedMax.Text = filament.FilamentParam.Split('|')[3];
                btnSave.Text = "Save";
                panel1.Visible = true;
            }
        }

        private void BtnDel_Click(object sender, EventArgs e)
        {
            int pos = MatDB.GetFilamentByName(materialType.Text).Position;
            if (pos > 11)
            {
                DialogResult result = MessageBox.Show("Are you sure you want to delete\n\n    " + materialType.Text, "Confirmation", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (result == DialogResult.Yes)
                {
                    MatDB.DeleteFilament(new Filament { Position = pos });
                    materialType.Items.Clear();
                    materialType.Items.AddRange(GetAllMaterials());
                    materialType.Text = materialType.Items[0].ToString();
                }
            }
        }

        private void BtnSave_Click(object sender, EventArgs e)
        {
            if (cboVendor.Text.Equals(String.Empty) || cboType.Text.Equals(String.Empty) ||
                txtSerial.Text.Equals(String.Empty) || txtExtMin.Text.Equals(String.Empty) ||
                txtExtMax.Text.Equals(String.Empty) || txtBedMin.Text.Equals(String.Empty) || txtBedMax.Text.Equals(String.Empty))
            {
                Toast.Show(this, "You must fill all fields", Toast.LENGTH_LONG, true);
                return;
            }

            panel1.Visible = false;

            if (btnSave.Text.Equals("Save"))
            {
                Filament filament = MatDB.GetFilamentByName(materialType.Text);

                filament.FilamentId = "";
                filament.FilamentName = cboVendor.Text.Trim() + " " + cboType.Text.Trim() + " " + txtSerial.Text.Trim();
                filament.FilamentVendor = "";
                filament.FilamentParam = txtExtMin.Text + "|" + txtExtMax.Text + "|" + txtBedMin.Text + "|" + txtBedMax.Text;

                MatDB.UpdateFilament(filament);

                materialType.Items.Clear();
                materialType.Items.AddRange(GetAllMaterials());
                materialType.Text = materialType.Items[filament.Position].ToString();

            }
            else
            {

                MatDB.AddFilament(new Filament { FilamentId = "", FilamentName = cboVendor.Text.Trim() + " " + cboType.Text.Trim() + " " + txtSerial.Text.Trim(), FilamentVendor = "", FilamentParam = txtExtMin.Text + "|" + txtExtMax.Text + "|" + txtBedMin.Text + "|" + txtBedMax.Text });
                materialType.Items.Clear();
                materialType.Items.AddRange(GetAllMaterials());
                materialType.Text = materialType.Items[MatDB.GetItemCount() - 1].ToString();
            }

        }

        private void MaterialType_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (materialType.SelectedIndex > 11)
            {
                btnDel.Visible = true;
                btnEdit.Visible = true;
            }
            else
            {
                btnDel.Visible = false;
                btnEdit.Visible = false;
            }

            Filament filament = MatDB.GetFilamentByName(materialType.Text);
            lblTemps.Text = String.Format(Resources.tempMessage, filament.FilamentParam.Split('|')[0], filament.FilamentParam.Split('|')[1], filament.FilamentParam.Split('|')[2], filament.FilamentParam.Split('|')[3]);
        }

        private void BtnCls_Click(object sender, EventArgs e)
        {
            panel1.Visible = false;
        }

        private void CboType_SelectedIndexChanged(object sender, EventArgs e)
        {
            txtExtMin.Text = GetDefaultTemps(cboType.Text)[0].ToString();
            txtExtMax.Text = GetDefaultTemps(cboType.Text)[1].ToString();
            txtBedMin.Text = GetDefaultTemps(cboType.Text)[2].ToString();
            txtBedMax.Text = GetDefaultTemps(cboType.Text)[3].ToString();
        }

        private void TxtExtMin_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar);
        }

        private void TxtExtMax_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar);
        }

        private void BtnAdd_MouseLeave(object sender, EventArgs e)
        {
            toolTip.Hide(btnAdd);
        }

        private void BtnDel_MouseLeave(object sender, EventArgs e)
        {
            toolTip.Hide(btnDel);
        }

        private void BtnEdit_MouseLeave(object sender, EventArgs e)
        {
            toolTip.Hide(btnEdit);
        }

        private void LblUid_Click(object sender, EventArgs e)
        {
            try
            {
                Clipboard.Clear();
                Clipboard.SetText("UID: " + lblUid.Text);
                Toast.Show(this, "UID copied to clipboard");
            }
            catch { }
        }

        private void LblUid_MouseLeave(object sender, EventArgs e)
        {
            balloonTip.Hide(lblUid);
        }

        private void ChkAutoRead_CheckedChanged(object sender, EventArgs e)
        {
            if (chkAutoRead.Checked)
            {
                chkAutoWrite.Checked = false;
            }
        }

        private void ChkAutoWrite_CheckedChanged(object sender, EventArgs e)
        {
            if (chkAutoWrite.Checked)
            {
                chkAutoRead.Checked = false;
            }
        }

        private void CboType_TextChanged(object sender, EventArgs e)
        {
            ComboBox comboBox = (ComboBox)sender;
            int pos = comboBox.SelectionStart;
            comboBox.Text = comboBox.Text.ToUpper();
            comboBox.SelectionStart = pos;
        }

        private void MainForm_LocationChanged(object sender, EventArgs e)
        {
            if (Toast.currentToastInstance != null && !Toast.currentToastInstance.IsDisposed)
            {
                Toast.currentToastInstance.UpdatePosition(this);
            }
        }

        private void TxtBedMin_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar);
        }


        private void TxtBedMax_KeyPress(object sender, KeyPressEventArgs e)
        {
            e.Handled = !char.IsDigit(e.KeyChar) && !char.IsControl(e.KeyChar);
        }
    }
}
