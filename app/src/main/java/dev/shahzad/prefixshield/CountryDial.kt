package dev.shahzad.prefixshield

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

data class DialCountry(val iso: String, val code: String, val name: String) {
    val dial: String get() = "+$code"

    val flag: String
        get() {
            val value = iso.uppercase()
            if (value.length != 2 || value.any { it !in 'A'..'Z' }) return ""
            val first = Character.toChars(0x1F1E6 + (value[0] - 'A'))
            val second = Character.toChars(0x1F1E6 + (value[1] - 'A'))
            return String(first) + String(second)
        }
}

object DialCountries {
    val all: List<DialCountry> by lazy {
        RAW.lineSequence()
            .mapNotNull { line ->
                val parts = line.split(',', limit = 3)
                if (parts.size < 3 || parts[0].isBlank()) null
                else DialCountry(parts[0].trim(), parts[1].trim(), parts[2].trim())
            }
            .sortedBy { it.name }
            .toList()
    }

    private val isoIndex: Map<String, DialCountry> by lazy {
        all.associateBy { it.iso.uppercase() }
    }

    fun forIso(iso: String?): DialCountry? = iso?.trim()?.uppercase()?.let { isoIndex[it] }

    fun detect(context: Context): DialCountry {
        val telephony = runCatching {
            val manager = context.getSystemService(TelephonyManager::class.java)
            sequenceOf(manager?.networkCountryIso, manager?.simCountryIso)
                .firstOrNull { !it.isNullOrBlank() && it.length == 2 }
        }.getOrNull()
        return forIso(telephony) ?: forIso(Locale.getDefault().country) ?: forIso("US") ?: all.first()
    }

    fun split(raw: String, fallback: DialCountry): Pair<DialCountry, String> {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return fallback to ""
        if (trimmed.startsWith("+")) {
            val matches = all.filter { digits.startsWith(it.code) && digits.length > it.code.length }
            val bestLen = matches.maxOfOrNull { it.code.length }
            if (bestLen != null) {
                val best = matches.filter { it.code.length == bestLen }
                val chosen = best.find { it.iso.equals(fallback.iso, true) } ?: best.first()
                return chosen to digits.drop(chosen.code.length)
            }
        }
        return fallback to digits
    }

    fun toE164(country: DialCountry, localRaw: String): String {
        val raw = localRaw.trim()
        if (raw.startsWith("+")) {
            val digits = raw.filter { it.isDigit() }
            return if (digits.isBlank()) "" else "+$digits"
        }
        var digits = raw.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        if (digits.startsWith("0")) digits = digits.drop(1)
        if (digits.startsWith(country.code) && digits.length > country.code.length + 7) return "+$digits"
        return "+${country.code}$digits"
    }

    private const val RAW = """
AF,93,Afghanistan
AL,355,Albania
DZ,213,Algeria
AR,54,Argentina
AM,374,Armenia
AU,61,Australia
AT,43,Austria
AZ,994,Azerbaijan
BH,973,Bahrain
BD,880,Bangladesh
BY,375,Belarus
BE,32,Belgium
BZ,501,Belize
BJ,229,Benin
BT,975,Bhutan
BO,591,Bolivia
BA,387,Bosnia and Herzegovina
BW,267,Botswana
BR,55,Brazil
BN,673,Brunei
BG,359,Bulgaria
KH,855,Cambodia
CM,237,Cameroon
CA,1,Canada
CL,56,Chile
CN,86,China
CO,57,Colombia
CR,506,Costa Rica
HR,385,Croatia
CU,53,Cuba
CY,357,Cyprus
CZ,420,Czechia
DK,45,Denmark
DO,1,Dominican Republic
EC,593,Ecuador
EG,20,Egypt
SV,503,El Salvador
EE,372,Estonia
ET,251,Ethiopia
FI,358,Finland
FR,33,France
GE,995,Georgia
DE,49,Germany
GH,233,Ghana
GR,30,Greece
GT,502,Guatemala
HN,504,Honduras
HK,852,Hong Kong
HU,36,Hungary
IS,354,Iceland
IN,91,India
ID,62,Indonesia
IR,98,Iran
IQ,964,Iraq
IE,353,Ireland
IL,972,Israel
IT,39,Italy
JM,1,Jamaica
JP,81,Japan
JO,962,Jordan
KZ,7,Kazakhstan
KE,254,Kenya
KW,965,Kuwait
KG,996,Kyrgyzstan
LA,856,Laos
LV,371,Latvia
LB,961,Lebanon
LY,218,Libya
LT,370,Lithuania
LU,352,Luxembourg
MO,853,Macau
MY,60,Malaysia
MV,960,Maldives
MT,356,Malta
MX,52,Mexico
MD,373,Moldova
MN,976,Mongolia
ME,382,Montenegro
MA,212,Morocco
MM,95,Myanmar
NP,977,Nepal
NL,31,Netherlands
NZ,64,New Zealand
NI,505,Nicaragua
NG,234,Nigeria
MK,389,North Macedonia
NO,47,Norway
OM,968,Oman
PK,92,Pakistan
PS,970,Palestine
PA,507,Panama
PY,595,Paraguay
PE,51,Peru
PH,63,Philippines
PL,48,Poland
PT,351,Portugal
PR,1,Puerto Rico
QA,974,Qatar
RO,40,Romania
RU,7,Russia
SA,966,Saudi Arabia
SN,221,Senegal
RS,381,Serbia
SG,65,Singapore
SK,421,Slovakia
SI,386,Slovenia
ZA,27,South Africa
KR,82,South Korea
ES,34,Spain
LK,94,Sri Lanka
SD,249,Sudan
SE,46,Sweden
CH,41,Switzerland
SY,963,Syria
TW,886,Taiwan
TJ,992,Tajikistan
TZ,255,Tanzania
TH,66,Thailand
TN,216,Tunisia
TR,90,Turkey
TM,993,Turkmenistan
UG,256,Uganda
UA,380,Ukraine
AE,971,United Arab Emirates
GB,44,United Kingdom
US,1,United States
UY,598,Uruguay
UZ,998,Uzbekistan
VE,58,Venezuela
VN,84,Vietnam
YE,967,Yemen
ZM,260,Zambia
ZW,263,Zimbabwe
"""
}
