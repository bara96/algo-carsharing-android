package com.example.algocarsharing

import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.algorand.algosdk.v2.client.common.Client
import com.algorand.algosdk.v2.client.common.IndexerClient
import com.algorand.algosdk.v2.client.common.Response
import com.algorand.algosdk.v2.client.model.ApplicationsResponse
import com.algorand.algosdk.v2.client.model.Enums
import com.algorand.algosdk.v2.client.model.TransactionsResponse
import com.example.algocarsharing.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            try {
                CoroutineScope(IO).launch {
                    //val jsonResp = async(Dispatchers.IO) { searchTransactions() }.await()
                    val jsonResp = withContext(IO) { searchTransactions() }
                    Log.d("Indexer response", jsonResp.toString(2))
                }

                Snackbar.make(view, "Ok", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            catch (e: Exception) {
                Log.e("Indexer error", e.stackTraceToString())
            }
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // utility function to connect to a node
    private fun connectToNetwork(): Client {
        val INDEXER_API_ADDR = "10.0.2.2"
        val INDEXER_API_PORT = 8980
        return IndexerClient(INDEXER_API_ADDR, INDEXER_API_PORT)
    }

    private suspend fun searchTransactions(): JSONObject {
        val note = "67c8df8c4a6ef03decdfd0f174d16641"
        val indexerClientInstance = connectToNetwork() as IndexerClient
        val response: Response<TransactionsResponse> = indexerClientInstance
            .searchForTransactions()
            .notePrefix(note.encodeToByteArray())
            .txType(Enums.TxType.APPL)
            .limit(100)
            .execute()

        if (!response.isSuccessful()) {
            Log.e("Indexer error: ", response.code().toString())
            throw Exception(response.message())
        }

        val jsonObj = JSONObject(response.body().toString())
        return jsonObj
    }

    private suspend fun searchApplication(id: Int): JSONObject {
        val indexerClientInstance = connectToNetwork() as IndexerClient
        val response: Response<ApplicationsResponse> = indexerClientInstance
            .searchForApplications()
            .applicationId(id.toLong())
            .limit(1)
            .execute()

        if (!response.isSuccessful()) {
            Log.e("Indexer error: ", response.code().toString())
            throw Exception(response.message())
        }

        val jsonObj = JSONObject(response.body().toString())
        return jsonObj
    }
}