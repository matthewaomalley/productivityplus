package lewisu.mattomalley.finalproject

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.MbmsDownloadSession.RESULT_CANCELLED
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingActionButton: FloatingActionButton
    private var currentCategoryFilter = "" // string for filtering notes

    // firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener : FirebaseAuth.AuthStateListener
    private lateinit var firebaseRecyclerAdapter: Adapter


    private var userId : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // reference delete all button
        val floatingActionButtonDeleteAll = findViewById<FloatingActionButton>(R.id.fab_delete_all)
        floatingActionButtonDeleteAll.setOnClickListener {
            deleteAllCompleteNotes()
        }

        userId = intent.getStringExtra("uid")

        recyclerView = findViewById(R.id.recycler_view)
        floatingActionButton = findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            val intent = Intent(applicationContext, DetailActivity::class.java)
            intent.putExtra("uid", userId)
            startActivity(intent)
        }

        val signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()

        // handle login
        ) { result: FirebaseAuthUIAuthenticationResult ->
            if (result.resultCode == RESULT_OK) {
                val user : FirebaseUser? = auth.currentUser
                if (user != null) {
                    userId = user.uid
                }
            } // close app if they cancel login
            if (result.resultCode == RESULT_CANCELLED){
                finish()
            }
        }

        // get user, and if logged in, get uid
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            userId = user.uid
        }
        setAdapter()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = auth.currentUser
            if (user != null) {
                userId = user.uid
                setAdapter()
            } else {
                val signInIntent = AuthUI.getInstance().createSignInIntentBuilder()
                    .setTheme(R.style.Theme_Productivity)
                    .setIsSmartLockEnabled(false)
                    .setAvailableProviders(listOf(AuthUI.IdpConfig.EmailBuilder().build()))
                    .build()

                signInLauncher.launch(signInIntent)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        auth.removeAuthStateListener (authStateListener)
    }

    override fun onResume() {
        super.onResume()
        auth.addAuthStateListener (authStateListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu) // inflate the menu

        val settingsMenuItem = menu?.findItem(R.id.settings_menu_item)
        settingsMenuItem?.setOnMenuItemClickListener {
            val popupMenu = PopupMenu(this, findViewById(R.id.settings_menu_item))
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) { // set the categoryFilter variable based on selection
                    R.id.filter_by_work -> filterByCategory("Work")
                    R.id.filter_by_home -> filterByCategory("Home")
                    R.id.filter_by_rec -> filterByCategory("Recreation")
                    R.id.filter_by_health -> filterByCategory("Health")
                    R.id.clear_filters -> filterByCategory("")
                }
                true
            }
            popupMenu.show()
            true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == R.id.sign_out){
            AuthUI.getInstance().signOut(this)
            true
        }else{
            super.onOptionsItemSelected(item)
        }
    }

    private fun filterByCategory(category: String) {
        currentCategoryFilter = category
        setAdapter()
    }

    // function to delete all notes that are marked as complete
    private fun deleteAllCompleteNotes() {
        val database = FirebaseDatabase.getInstance() // get instance of database
        database.reference.child("to_do")
            .get()
            .addOnSuccessListener { snapshot ->
                val completeNotes = mutableListOf<DataSnapshot>()
                snapshot.children.forEach { noteSnapshot ->
                    val isNoteComplete = noteSnapshot.child("complete").getValue(Boolean::class.java)
                    if (isNoteComplete == true) { // add completed notes to a list if they are 'complete'
                        completeNotes.add(noteSnapshot)
                    }
                }
                if (completeNotes.isEmpty()) { // if the list is empty, then display Toast
                    Toast.makeText(applicationContext, "There are no complete notes!", Toast.LENGTH_SHORT).show()
                } else { // if it's not empty (at least one is complete) then delete
                    AlertDialog.Builder(this) // create a dialog box that asks users to confirm they want to delete
                        .setTitle("Delete Complete Notes")
                        .setMessage("Are you sure you want to delete all complete notes?")
                        .setPositiveButton("Yes") { _, _ -> // if they click yes, delete
                            snapshot.children.forEach { noteSnapshot ->
                                val isNoteComplete = noteSnapshot.child("complete").getValue(Boolean::class.java)
                                if (isNoteComplete == true) { // if each note in list is complete, remove it from the database
                                    noteSnapshot.ref.removeValue()
                                }
                            }
                            Toast.makeText(applicationContext, "Note(s) deleted!", Toast.LENGTH_SHORT).show() // toast notes were deleted
                        }
                        .setNegativeButton("No", null) // close dialog box and do nothing if user clicks No
                        .show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed.", exception)
                Toast.makeText(applicationContext, "Failed to delete notes.", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("CutPasteId")
    private fun setAdapter() {
        val onClickListener = { position: Int ->
            val detailIntent = Intent(this, DetailActivity::class.java)
            detailIntent.putExtra("uid", userId)
            val ref: DatabaseReference = firebaseRecyclerAdapter.getRef(position)
            val id = ref.key
            detailIntent.putExtra("ref", id)
            startActivity(detailIntent)
        }

        val firebaseDatabase = FirebaseDatabase.getInstance() // create instance of database
        // create a query
        val baseQuery = firebaseDatabase.reference.child("to_do").orderByChild("uid").equalTo(userId)

        // Filter the notes based on category
        val optionsBuilder = FirebaseRecyclerOptions.Builder<ListItem>()
        var query = baseQuery
        if (currentCategoryFilter.isNotEmpty()) { // if a category filter has been applied
            // reassign the query to a new reference that orders notes by category
            query = FirebaseDatabase.getInstance().getReference("to_do").orderByChild("category").equalTo(currentCategoryFilter)
        }
        optionsBuilder.setQuery(query, ListItem::class.java)
        val options = optionsBuilder.build()

        val filterMenu = findViewById<View>(R.id.menu_text) as? MenuItem
        if (filterMenu != null) {

            filterMenu.isEnabled = true

            filterMenu.setOnMenuItemClickListener {
                val popupMenu = PopupMenu(this, findViewById(R.id.menu_text))
                popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) { // check the itemId of the selected filter item
                        // set the query of optionsBuilder to filter the listItem based on their category
                        R.id.filter_by_work -> optionsBuilder.setQuery(query.equalTo("Work", "category"), ListItem::class.java)
                        R.id.filter_by_home -> optionsBuilder.setQuery(query.equalTo("Home", "category"), ListItem::class.java)
                        R.id.filter_by_rec -> optionsBuilder.setQuery(query.equalTo("Recreation", "category"), ListItem::class.java)
                        R.id.filter_by_health -> optionsBuilder.setQuery(query.equalTo("Health", "category"), ListItem::class.java)
                        R.id.clear_filters -> optionsBuilder.setQuery(query, ListItem::class.java)
                    }

                    firebaseRecyclerAdapter.updateOptions(optionsBuilder.build())
                    true
                }
                popupMenu.show()
                true
            }

        }

        firebaseRecyclerAdapter = Adapter(options, onClickListener)
        firebaseRecyclerAdapter.startListening()
        recyclerView.adapter = firebaseRecyclerAdapter
    }
}