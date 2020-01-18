package com.blockdevs.blockdevs

import android.app.DownloadManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.DefaultCardManagerDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.activity_main.*
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Server

class MainActivity : AppCompatActivity() {

    // Initialize SDK Functions to interact with the Tangem Cards
    private val nfcManager = NfcManager()
    private val cardManagerDelegate : DefaultCardManagerDelegate =  DefaultCardManagerDelegate(nfcManager.reader)
    private val cardManager = CardManager(nfcManager.reader, cardManagerDelegate)


    // Lazy initialization
    private lateinit var cardId: String


    // Storing data that needs safe calls or null checks
    // Protection agains NullPointerException for those who knows java
    private var wallet: String? = String()
    private var wallet_public_key : ByteArray? = null


    // Descriptions
    private val cardIsCancelled = "User cancelled!"

    private fun refreshTotal() {
        val numPendingCoffee: Int = if (pending_coffees.text == null || pending_coffees.text.toString() == "") {
            0
        } else {
            pending_coffees.text.toString().toInt()
        }
        val numCoffees: Int = if (coffees.text == null || coffees.text.toString() == "") {
            0
        } else {
            coffees.text.toString().toInt()
        }
        total.text = (numPendingCoffee + numCoffees).toString()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add listeners to keep "total" up to date
        pending_coffees.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                refreshTotal()
            }
        })

        coffees.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                refreshTotal()
            }
        })

        // Interact with Android NFC
        nfcManager.setCurrentActivity(this)
        cardManagerDelegate.activity = this
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        // Trigger scan card method on button click
        read_card?.setOnClickListener { _ ->
            cardManager.scanCard { taskEvent ->
                when (taskEvent) {
                    is TaskEvent.Event -> {
                        when (taskEvent.data) {
                            is ScanEvent.OnReadEvent -> {

                                /**
                                 * cardID is used in various SDK Tasks and functions.
                                 * It returns a string that you can store on a variable (declared within the class)
                                 */
                                cardId = (taskEvent.data as ScanEvent.OnReadEvent).card.cardId


                                /**
                                 * In getting stellar wallet address:
                                 * 1.) Convert the wallet public key (in BytesArray) to KeyPair from the stellar SDK
                                 * 2.) Use the getAccountId() method from the stellar SDK to obtain the wallet address
                                 */

                                // 1.) Convert the wallet public key to KeyPair
                                var keypair: KeyPair =
                                    KeyPair.fromPublicKey((taskEvent.data as ScanEvent.OnReadEvent).card.walletPublicKey!!)

                                // 2.) Use getAccountId() to get the wallet address and store it if you like
                                wallet = keypair.getAccountId()

                                // Referencing the public key to use for signing
                                wallet_public_key = (taskEvent.data as ScanEvent.OnReadEvent).card.walletPublicKey!!

                            }

                            is ScanEvent.OnVerifyEvent -> {
                                // Handle card verification and display data
                                runOnUiThread {
                                    // display text
                                    status?.text = "Hi, " + cardId + "!"
                                    txt_wallet?.text = wallet
                                    btn_sign.isEnabled = true
                                }
                            }
                        }
                    }
                    is TaskEvent.Completion -> {
                        if (taskEvent.error != null) {
                            if (taskEvent.error is TaskError.UserCancelledError) {
                                // Handle case when user cancelled manually
                                runOnUiThread {
                                    status?.text = cardIsCancelled
                                }
                            }
                            // Handle other errors

                        }
                        // Handle completion
                        // Call on async task to connect to the stellar network
                        Stellar(wallet, this).execute()
                    }
                }

                btn_sign?.setOnClickListener { _ ->
                    cardManager.sign(
                        createSampleHashes(),
                        cardId
                    ) {
                        when (it) {
                            is TaskEvent.Completion -> {
                                if (it.error != null) runOnUiThread {
                                    status?.text = it.error!!::class.simpleName
                                }
                            }
                            is TaskEvent.Event -> runOnUiThread {
                                status?.text = cardId + " used to sign sample hashes."
                            }
                        }
                    }

                }
            }

        }
    }


    // Creates sample hashed transactions to sign
        private fun createSampleHashes(): Array<ByteArray> {
            val hash1 = ByteArray(32) { 1 }
            val hash2 = ByteArray(32) { 2 }
            return arrayOf(hash1, hash2)
        }

}

// Async task to talk to the stellar network
class Stellar(val wallet_address: String?, private var activity: MainActivity?) : AsyncTask<Void, Void, String>() {
    private lateinit var wallet_balance: String

    // Point to the stellar main network you can also point it to the test network if you wish
    var server = Server("https://horizon.stellar.org")


    override fun doInBackground(vararg params: Void?): String? {
        // Connect to the server and get all balances of the wallet address
        val balances = server.accounts().account(wallet_address).getBalances()

        // Enumerate balances of the addresses
        for (balance in balances) {
            // get the total XLM (native asset) balance
            if (balance.assetType.equals("native", ignoreCase = true)) {
                wallet_balance = balance.balance
            }
        }

        return wallet_balance
    }

    override fun onPreExecute() {
        super.onPreExecute()

        // If you want to initalize anything

    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        // Run balance on thread
        activity?.txt_wallet2?.text = result
    }
}