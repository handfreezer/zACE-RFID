using PCSC;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

namespace ACE_RFID
{
    public class Reader
    {
        private readonly ICardReader reader;

        public Reader(ICardReader icReader)
        {
            reader = icReader ?? throw new ArgumentNullException(nameof(icReader));
        }

        public byte[] GetData()
        {
            byte[] response = new byte[10];
            if (reader.Transmit(new byte[] { 0xFF, 0xCA, 0x00, 0x00, 0x00 }, response) < 8)
            {
                return null;
            }
            Array.Resize(ref response, 6);
            return response;
        }

        public byte[] ReadBinaryBlocks(int block, int len)
        {
            byte[] response = new byte[len + 2];
            reader.Transmit(new byte[] { 0xFF, 0xB0, 0x00, (byte)block, (byte)len }, response);
            Array.Resize(ref response, len);
            return response;
        }

        public bool UpdateBinaryBlocks(int block, int len, byte[] blockData)
        {
            byte[] response = new byte[2];
            List<byte> command = new byte[] { 0xFF, 0xD6, 0x00, (byte)block, (byte)len }.ToList();
            command.AddRange(blockData);
            reader.Transmit(command.ToArray(), response);
            return (response[0] == 0x90) && (response[1] == 0x00);
        }

        public byte[] GetFirmwareVersion()
        {
            byte[] response = new byte[12];
            reader.Transmit(new byte[] { 0xFF, 0x00, 0x48, 0x00, 0x00 }, response);
            Array.Resize(ref response, 10);
            return response;
        }

        public bool SetBuzzerOutputduringCardDetection(bool on)
        {
            byte[] response = new byte[2];
            reader.Transmit(new byte[] { 0xFF, 0x00, 0x52, (byte)(on ? 0xff : 0x00), 0x00 }, response);
            return (response[0] == 0x90) && (response[1] == 0x00);
        }

        public bool WriteData(byte[] data)
        {
            Array.Resize(ref data, 144);

            int pos = 0;
            while (pos < data.Length)
            {
                byte[] buf = new byte[4];
                int len = data.Length - pos > 4 ? 4 : data.Length - pos;
                Array.Copy(data, pos, buf, 0, len);

                UpdateBinaryBlocks((pos / 4) + 4, 4, buf);

                pos += 4;
            }
            Thread.Sleep(200);
            byte[] readback = ReadData();
            return data.SequenceEqual(readback);
        }

        public byte[] ReadData()
        {
            List<byte> data = new List<byte>();
            int pos = 0;
            while (pos < 144)
            {
                int len = 144 - pos > 4 ? 4 : 144 - pos;
                byte[] buf = ReadBinaryBlocks((pos / 4) + 4, len);
                data.AddRange(buf);
                pos += 4;
            }
            return data.ToArray();
        }
    }
}