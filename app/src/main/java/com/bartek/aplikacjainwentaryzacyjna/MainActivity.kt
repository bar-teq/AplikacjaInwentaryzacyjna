package com.bartek.aplikacjainwentaryzacyjna

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var menuButton: Button
    private lateinit var statusButton: Button
    private lateinit var missingButton: Button
    private lateinit var selectFileButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var infoTextView: TextView
    private lateinit var machineAdapter: MachineAdapter

    private var allMachines: MutableList<Machine> = mutableListOf()
    private var presentMachinesIds: MutableSet<String> = mutableSetOf()
    private var currentFilter: String = ""
    private var isShowingMissing = false

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { loadMachinesFromHtml(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchInput = findViewById(R.id.search_input)
        menuButton = findViewById(R.id.search_button) // zmieniony tekst na "Menu główne"
        statusButton = findViewById(R.id.status_button)
        missingButton = findViewById(R.id.missing_button)
        selectFileButton = findViewById(R.id.select_file_button)
        recyclerView = findViewById(R.id.recycler_view)
        infoTextView = findViewById(R.id.info_text_view)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = false
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

        machineAdapter = MachineAdapter(allMachines, presentMachinesIds) { machine ->
            if (presentMachinesIds.contains(machine.id)) presentMachinesIds.remove(machine.id)
            else presentMachinesIds.add(machine.id)
            if (!isShowingMissing) filterMachines(currentFilter)
            else showMissingMachines() // odśwież listę brakujących
        }
        recyclerView.adapter = machineAdapter

        menuButton.setOnClickListener {
            // Zawsze powrót do głównej listy
            isShowingMissing = false
            menuButton.setBackgroundColor(0xFFA8E6A3.toInt()) // zielony
            missingButton.setBackgroundColor(0xFFFFFFFF.toInt()) // biały
            filterMachines(currentFilter)
            infoTextView.text = "Lista maszyn"
        }

        statusButton.setOnClickListener {
            val statuses = allMachines.map { it.status }.distinct()
            val statusArray = statuses.toTypedArray()
            val checkedItems = BooleanArray(statusArray.size) { false }

            AlertDialog.Builder(this)
                .setTitle("Wybierz statusy")
                .setMultiChoiceItems(statusArray, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    allMachines.forEach { machine ->
                        val index = statuses.indexOf(machine.status)
                        if (index >= 0 && checkedItems[index]) {
                            presentMachinesIds.add(machine.id)
                        }
                    }
                    if (!isShowingMissing) filterMachines(currentFilter)
                    else showMissingMachines()
                    infoTextView.text = "Zaznaczono maszyny dla wybranych statusów."
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }

        missingButton.setOnClickListener {
            isShowingMissing = true
            missingButton.setBackgroundColor(0xFFFFA3A3.toInt()) // czerwony
            menuButton.setBackgroundColor(0xFFFFFFFF.toInt()) // biały
            showMissingMachines()
        }

        selectFileButton.setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }

        val numberButtons = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        )

        for (id in numberButtons) {
            findViewById<Button>(id).setOnClickListener {
                val digit = (it as Button).text.toString()
                currentFilter += digit
                searchInput.setText(currentFilter)
                if (!isShowingMissing) filterMachines(currentFilter)
                else showMissingMachines()
            }
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            currentFilter = ""
            searchInput.setText("")
            if (!isShowingMissing) filterMachines(currentFilter)
            else showMissingMachines()
        }

        findViewById<Button>(R.id.btn_ok).setOnClickListener {
            // dodatkowa akcja OK jeśli potrzeba
        }
    }

    private fun filterMachines(query: String) {
        val filtered = if (query.isEmpty()) allMachines
        else allMachines.filter { it.id.contains(query) }

        machineAdapter.updateData(filtered)
    }

    private fun showMissingMachines() {
        val missingMachines = allMachines.filter { !presentMachinesIds.contains(it.id) }

        // deklarujemy adapter jako zmienną mutable
        var missingAdapter: MachineAdapter? = null

        missingAdapter = MachineAdapter(missingMachines, presentMachinesIds) { machine ->
            presentMachinesIds.add(machine.id)
            val updatedMissing = missingMachines.filter { it.id != machine.id }
            missingAdapter?.updateData(updatedMissing)
            infoTextView.text = "Maszyna ${machine.id} oznaczona jako obecna"
        }

        recyclerView.adapter = missingAdapter
        infoTextView.text = "Lista brakujących maszyn"
    }

    private fun loadMachinesFromHtml(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val html = stream.bufferedReader().use { it.readText() }
                val doc = Jsoup.parse(html)
                val table = doc.selectFirst("table") ?: run {
                    infoTextView.text = "Nie znaleziono tabeli w pliku."
                    return
                }
                val newMachines = mutableListOf<Machine>()
                val rows = table.select("tr")
                for (i in 2 until rows.size) {
                    val cols = rows[i].select("td")
                    if (cols.size >= 8) {
                        val id = cols[2].text().trim()
                        val name = cols[3].text().trim()
                        val status = cols[7].text().trim()
                        newMachines.add(Machine(id, name, status))
                    }
                }
                allMachines.clear()
                allMachines.addAll(newMachines)
                presentMachinesIds.clear()
                currentFilter = ""
                searchInput.setText("")
                filterMachines(currentFilter)
            }
        } catch (e: Exception) {
            infoTextView.text = "Błąd wczytywania pliku: ${e.message}"
        }
    }
}
