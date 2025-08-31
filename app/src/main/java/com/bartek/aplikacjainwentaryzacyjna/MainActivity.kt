package com.bartek.aplikacjainwentaryzacyjna

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var statusButton: Button
    private lateinit var missingButton: Button
    private lateinit var selectFileButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var infoTextView: TextView
    private lateinit var machineAdapter: MachineAdapter

    private var allMachines: MutableList<Machine> = mutableListOf()
    private var presentMachinesIds: MutableSet<String> = mutableSetOf()
    private var currentFilter: String = ""

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                loadMachinesFromHtml(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)
        statusButton = findViewById(R.id.status_button)
        missingButton = findViewById(R.id.missing_button)
        selectFileButton = findViewById(R.id.select_file_button)
        recyclerView = findViewById(R.id.recycler_view)
        infoTextView = findViewById(R.id.info_text_view)

        machineAdapter = MachineAdapter(allMachines, presentMachinesIds) { machine ->
            // kliknięcie nie resetuje filtra
            if (presentMachinesIds.contains(machine.id)) {
                presentMachinesIds.remove(machine.id)
            } else {
                presentMachinesIds.add(machine.id)
            }
            filterMachines(currentFilter)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = machineAdapter

        searchButton.setOnClickListener {
            val searchTerm = searchInput.text.toString().trim()
            if (searchTerm.isNotEmpty()) searchForMachine(searchTerm)
        }

        statusButton.setOnClickListener {
            val statusText = if (presentMachinesIds.isEmpty()) {
                "Nie oznaczono jeszcze żadnych maszyn."
            } else {
                "Obecne maszyny: ${presentMachinesIds.joinToString(", ")}"
            }
            infoTextView.text = statusText
        }

        missingButton.setOnClickListener {
            val missing = allMachines.filter { !presentMachinesIds.contains(it.id) }
            val text = if (missing.isEmpty()) {
                "Brakujące: brak"
            } else {
                "Brakujące maszyny: ${missing.joinToString { it.id }}"
            }
            infoTextView.text = text
        }

        val numberButtons = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        )

        for (id in numberButtons) {
            findViewById<Button>(id).setOnClickListener {
                val current = searchInput.text.toString()
                val digit = (it as Button).text.toString()
                val newInput = current + digit
                searchInput.setText(newInput)
                currentFilter = newInput
                filterMachines(currentFilter)
            }
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            searchInput.setText("")
            currentFilter = ""
            filterMachines(currentFilter)
        }

        findViewById<Button>(R.id.btn_ok).setOnClickListener {
            val searchTerm = searchInput.text.toString().trim()
            if (searchTerm.isNotEmpty()) searchForMachine(searchTerm)
        }

        selectFileButton.setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }
    }

    private fun filterMachines(query: String) {
        val filtered = if (query.isEmpty()) allMachines else allMachines.filter { it.id.contains(query) }
        machineAdapter.updateData(filtered)
    }

    private fun searchForMachine(searchTerm: String) {
        val found = allMachines.find { it.id == searchTerm }
        if (found != null) {
            if (presentMachinesIds.contains(found.id)) {
                presentMachinesIds.remove(found.id)
                infoTextView.text = "Maszyna ${found.id} oznaczona jako brakująca."
            } else {
                presentMachinesIds.add(found.id)
                infoTextView.text = "Maszyna ${found.id} oznaczona jako obecna."
            }
            filterMachines(currentFilter) // nie resetujemy filtra
        } else {
            infoTextView.text = "Nie znaleziono maszyny o ID: $searchTerm"
        }
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
                        val category = cols[1].text().trim()
                        val status = cols[7].text().trim()
                        if (id.isNotEmpty() && name.isNotEmpty()) {
                            newMachines.add(Machine(id, name, category, status))
                        }
                    }
                }

                allMachines.clear()
                allMachines.addAll(newMachines)
                currentFilter = ""
                filterMachines(currentFilter) // pokazuje pełną listę od razu
                infoTextView.text = "Wczytano ${allMachines.size} maszyn z wybranego pliku."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            infoTextView.text = "Błąd przy wczytywaniu pliku: ${e.message}"
        }
    }
}
