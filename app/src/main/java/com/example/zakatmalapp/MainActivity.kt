
package com.example.zakatmalapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val rupiahFormat = NumberFormat.getCurrencyInstance(
    Locale("id", "ID")
).apply {
    maximumFractionDigits = 0
    minimumFractionDigits = 0
}

private fun rupiah(nilai: Double): String =
    rupiahFormat.format(nilai)

private fun parseNominal(teks: String): Double? {
    if (teks.isBlank()) return null
    val nilai = teks.toDoubleOrNull() ?: return null
    if (!nilai.isFinite() || nilai < 0.0) return null
    return nilai
}

private val jenisHarta = listOf(
    "Tabungan / uang tunai",
    "Emas",
    "Barang dagangan",
    "Piutang yang dapat ditagih",
    "Berlian",
    "Rumah"
)

private data class ItemHarta(
    val id: Int,
    val jenis: String,
    val nominal: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ZakatMalApp()
                }
            }
        }
    }
}

@Composable
fun ZakatMalApp() {
    val daftarHarta = remember {
        mutableStateListOf<ItemHarta>()
    }

    var hargaEmas by remember { mutableStateOf("2243000") }
    var nominalHarta by remember { mutableStateOf("") }
    var nominalUtang by remember { mutableStateOf("0") }
    var pilihanJenis by remember {
        mutableStateOf(jenisHarta.first())
    }
    var haulTerpenuhi by remember { mutableStateOf(false) }
    var pesan by remember { mutableStateOf<String?>(null) }
    var hasilDihitung by remember { mutableStateOf(false) }
    var idBerikutnya by remember { mutableIntStateOf(1) }

    val harga = parseNominal(hargaEmas)
    val utang = parseNominal(nominalUtang)
    val totalHarta = daftarHarta.sumOf { it.nominal }
    val hartaBersih = totalHarta - (utang ?: 0.0)
    val nisab = (harga ?: 0.0) * 84.0

    val inputValid = harga != null &&
            harga > 0.0 &&
            utang != null &&
            daftarHarta.isNotEmpty() &&
            hartaBersih >= 0.0

    val memenuhiNisab = inputValid && hartaBersih >= nisab
    val wajibZakat = memenuhiNisab && haulTerpenuhi
    val jumlahZakat = if (wajibZakat) {
        hartaBersih * 0.050
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Kalkulator Zakat Mal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Hitung dan periksa kewajiban zakat harta.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                BagianJudul("1. Harga emas dan nisab")

                OutlinedTextField(
                    value = hargaEmas,
                    onValueChange = {
                        hargaEmas = it
                        hasilDihitung = false
                        pesan = null
                    },
                    label = { Text("Harga emas per gram (Rp)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                RingkasanBaris(
                    "Nisab (85 gram emas)",
                    rupiah(nisab)
                )
            }

            item {
                HorizontalDivider()
                BagianJudul("2. Tambahkan harta")

                PilihanJenisHarta(
                    pilihan = pilihanJenis,
                    saatDipilih = {
                        pilihanJenis = it
                    }
                )

                OutlinedTextField(
                    value = nominalHarta,
                    onValueChange = {
                        nominalHarta = it
                        pesan = null
                    },
                    label = { Text("Nilai harta (Rp)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val nominal = parseNominal(nominalHarta)

                        when {
                            nominal == null ->
                                pesan = "Masukkan nilai harta berupa angka positif atau nol."

                            nominal <= 0.0 ->
                                pesan = "Nilai harta harus lebih dari nol."

                            else -> {
                                daftarHarta.add(
                                    ItemHarta(
                                        id = idBerikutnya++,
                                        jenis = pilihanJenis,
                                        nominal = nominal
                                    )
                                )
                                nominalHarta = ""
                                hasilDihitung = false
                                pesan = "Harta berhasil ditambahkan."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tambah Harta")
                }
            }

            if (daftarHarta.isNotEmpty()) {
                item {
                    BagianJudul("Daftar harta")

                    Text(
                        "Tekan Hapus pada item yang ingin dihapus.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                items(
                    items = daftarHarta,
                    key = { it.id }
                ) { item ->
                    KartuHarta(
                        item = item,
                        saatHapus = {
                            daftarHarta.remove(item)
                            hasilDihitung = false
                            pesan = "Harta berhasil dihapus."
                        }
                    )
                }
            }

            item {
                HorizontalDivider()
                BagianJudul("3. Utang jatuh tempo")

                OutlinedTextField(
                    value = nominalUtang,
                    onValueChange = {
                        nominalUtang = it
                        hasilDihitung = false
                        pesan = null
                    },
                    label = { Text("Total utang jatuh tempo (Rp)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "Utang jatuh tempo akan dikurangkan dari total harta.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                HorizontalDivider()
                BagianJudul("4. Ketentuan haul")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = haulTerpenuhi,
                        onCheckedChange = {
                            haulTerpenuhi = it
                            hasilDihitung = false
                            pesan = null
                        }
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Harta telah dimiliki selama satu tahun hijriah")
                        Text(
                            "Centang jika syarat haul telah terpenuhi.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        when {
                            harga == null || harga <= 0.0 ->
                                pesan = "Harga emas harus berupa angka lebih dari nol."

                            utang == null ->
                                pesan = "Utang harus berupa angka nol atau lebih."

                            daftarHarta.isEmpty() ->
                                pesan = "Tambahkan minimal satu item harta."

                            hartaBersih < 0.0 ->
                                pesan = "Utang tidak boleh melebihi total harta."

                            !memenuhiNisab ->
                                pesan = "Harta bersih belum mencapai nisab."

                            !haulTerpenuhi ->
                                pesan = "Nisab tercapai, tetapi haul belum terpenuhi."

                            else ->
                                pesan = "Perhitungan selesai."
                        }

                        hasilDihitung = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hitung Zakat")
                }
            }

            if (hasilDihitung) {
                item {
                    KartuHasil(
                        totalHarta = totalHarta,
                        utang = utang ?: 0.0,
                        hartaBersih = hartaBersih,
                        nisab = nisab,
                        haulTerpenuhi = haulTerpenuhi,
                        memenuhiNisab = memenuhiNisab,
                        wajibZakat = wajibZakat,
                        jumlahZakat = jumlahZakat,
                        pesan = pesan
                    )
                }
            } else if (pesan != null) {
                item {
                    Text(
                        text = pesan!!,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BagianJudul(judul: String) {
    Text(
        text = judul,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun RingkasanBaris(
    label: String,
    nilai: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            nilai,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PilihanJenisHarta(
    pilihan: String,
    saatDipilih: (String) -> Unit
) {
    var terbuka by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(pilihan)
        }

        DropdownMenu(
            expanded = terbuka,
            onDismissRequest = { terbuka = false }
        ) {
            jenisHarta.forEach { jenis ->
                DropdownMenuItem(
                    text = { Text(jenis) },
                    onClick = {
                        saatDipilih(jenis)
                        terbuka = false
                    }
                )
            }
        }
    }
}

@Composable
private fun KartuHarta(
    item: ItemHarta,
    saatHapus: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.jenis,
                    fontWeight = FontWeight.Bold
                )
                Text(rupiah(item.nominal))
            }

            TextButton(onClick = saatHapus) {
                Text("Hapus")
            }
        }
    }
}

@Composable
private fun KartuHasil(
    totalHarta: Double,
    utang: Double,
    hartaBersih: Double,
    nisab: Double,
    haulTerpenuhi: Boolean,
    memenuhiNisab: Boolean,
    wajibZakat: Boolean,
    jumlahZakat: Double,
    pesan: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Hasil Perhitungan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            RingkasanBaris("Total harta", rupiah(totalHarta))
            RingkasanBaris("Utang jatuh tempo", rupiah(utang))
            RingkasanBaris("Harta bersih", rupiah(hartaBersih))
            RingkasanBaris("Nisab", rupiah(nisab))

            HorizontalDivider()

            Text(
                when {
                    hartaBersih < 0.0 ->
                        "Harta bersih tidak valid."

                    !memenuhiNisab ->
                        "Status: Belum memenuhi nisab."

                    !haulTerpenuhi ->
                        "Status: Nisab tercapai, haul belum terpenuhi."

                    else ->
                        "Status: Memenuhi nisab dan haul."
                },
                fontWeight = FontWeight.Bold
            )

            if (wajibZakat) {
                Text(
                    "Estimasi zakat (2,5%): ${rupiah(jumlahZakat)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("Estimasi zakat wajib: Rp0")
            }

            if (pesan != null) {
                Text(pesan)
            }

            Text(
                "Hasil ini merupakan estimasi. Pastikan jenis harta, " +
                        "ketentuan haul, utang yang dapat dikurangkan, " +
                        "dan ketentuan zakat sesuai dengan panduan " +
                        "lembaga zakat yang kamu ikuti.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
