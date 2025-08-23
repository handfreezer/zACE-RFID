# ACE-RFID
Anycubic ACE RFID Programming.<br>
<br>

The tags required are MIFARE <a href=https://www.nxp.com/products/rfid-nfc/mifare-hf/mifare-ultralight/mifare-ultralight-c:MF0ICU2>Ultralight C</a> compatible tags.<br>
<a href=https://www.nxp.com/products/NTAG213_215_216>NTAG215</a> tags have been tested and work.<br>
<a href=https://www.nxp.com/products/NTAG213_215_216>NTAG213</a> tags have been tested and work.<br><br>
Tags need at least 36 pages, 144 byte user r/w area.<br>

<br>
<br>
<a href=https://github.com/DnG-Crafts/ACE-RFID/tree/main/Android/AceRFID>Android Code</a>
<br>
<br>

<a href=https://github.com/DnG-Crafts/ACE-RFID/tree/main/Arduino>Arduino Code</a>
<br>
<br>
<a href=https://github.com/DnG-Crafts/ACE-RFID/tree/main/Windows>Windows Code</a>
<br>
<br>

[![https://www.youtube.com/watch?v=HWLOISYwv88](https://img.youtube.com/vi/HWLOISYwv88/0.jpg)](https://www.youtube.com/watch?v=HWLOISYwv88)

https://www.youtube.com/watch?v=HWLOISYwv88<br>


<br><br><br>
The android app is available on google play<br>
<a href="https://play.google.com/store/apps/details?id=dngsoftware.acerfid&hl=en"><img src=https://github.com/DnG-Crafts/ACE-RFID/blob/main/docs/gp.webp width="30%" height="30%"></a>
<br>



## Tag Format

<br>

Response from ACE ( <a href=https://github.com/DnG-Crafts/ACE-RFID/blob/main/docs/N033%20Material%20box%20communication%20protocol.md>N033</a> )<br>

```
{ 
    "index": 0,          // material number, starting from 0 
    "sku": "AHPLLB-103", // SKU 
    "brand": "AC",       // brand 
    "type": "PLA",       // Type of consumables PLA+ TPU ABS PETG 
    "source": 1          // Source of consumable information, 0-unknown, 1-RFID, 2-user definition, 3-no material 
    "color": [           // [RGB color] 
        0,               // Red 
        255,             // green 
        0                // Blue 
    ], 
    "extruder_temp": {   // recommended temperature for extruder 
        "min": 200,      // minimum temperature 
        "max": 210       // maximum temperature 
    }, 
    "hotbed_temp": {     // hotbed recommended temperature 
        "min": 50,       // minimum temperature 
        "max": 60        // maximum temperature 
    }, 
    "diameter": 1.75,    // Consumable diameter, unit mm 
	"total": 330,    // Consumable length, unit m 
	"current": 0     // Consumable remaining, unit m 
} 
```

<br>
<br>



Tag page structure<br>

```
| Page |      Data     |
|   4  |  7B 00 65 00  |          magic byte? / data len?
|      |               |
|   5  |  41 48 50 4C  |  AHPL    sku
|   6  |  4C 42 2D 31  |  LB-1    sku
|   7  |  30 33 00 00  |  03      sku
|   8  |  00 00 00 00  |          sku
|      |               |
|  10  |  41 43 00 00  |  AC      brand
|  11  |  00 00 00 00  |          brand
|  12  |  00 00 00 00  |          brand
|  13  |  00 00 00 00  |          brand
|      |               |
|  15  |  50 4C 41 00  |  PLA     type
|  16  |  00 00 00 00  |          type
|  17  |  00 00 00 00  |          type
|  18  |  00 00 00 00  |          type
|      |               | 
|  20  |  FF 00 FF 00  |  FF00FF00  abgr color
|      |               |
|  24  |  C8 00 D2 00  |  extruder_temp [ min C8 00 / max  D2 00 ]
|      |               |
|  29  |  32 00 3C 00  |  hotbed_temp [ min 32 00 / max  3C 00 ]
|      |               |
|  30  |  AF 00 4A 01  |  filament param [ diameter  AF 00  /  length  4A 01 ]
|      |               |
|  31  |  E8 03 00 00  |  unknown
```

<br>





# Arduino

<a href=https://github.com/DnG-Crafts/ACE-RFID/tree/main/Arduino/ESP32>Code for ESP32 boards</a><br>
<a href=https://github.com/DnG-Crafts/ACE-RFID/tree/main/Arduino/ESP8266>Code for ESP8266 boards</a><br>
<a href=https://github.com/DnG-Crafts/ACE-RFID/tree/main/Arduino/Pico_W>Code for Pico W boards</a><br>

<br><br>

Flash Code:

[![https://www.youtube.com/watch?v=WOznbT7NbWI](https://img.youtube.com/vi/WOznbT7NbWI/0.jpg)](https://www.youtube.com/watch?v=WOznbT7NbWI)

https://www.youtube.com/watch?v=WOznbT7NbWI<br>