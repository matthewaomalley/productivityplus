package lewisu.mattomalley.finalproject

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*


class DetailActivity: AppCompatActivity() {

    // initialize variables
    private var listItem: ListItem? = ListItem()
    private lateinit var titleEditText: EditText
    private lateinit var descEditText: EditText
    private lateinit var priorityBar: RatingBar
    private lateinit var categorySpinner: Spinner
    private lateinit var completeCheckBox: CheckBox
    private lateinit var addEditButton: Button
    private var userId: String? = null
    private var reference: String? = null

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_detail)

        // set the rating value for the detail screen based on number of stars
        val ratingBar = findViewById<RatingBar>(R.id.priority_bar)
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            when (rating) {
                in 0.5..1.5 -> ratingBar.setStarColor(ContextCompat.getColor(this, R.color.ratingYellow))
                in 2.0..3.5 -> ratingBar.setStarColor(ContextCompat.getColor(this, R.color.ratingOrange))
                in 4.0..5.0 -> ratingBar.setStarColor(ContextCompat.getColor(this, R.color.ratingRed))
                else -> ratingBar.setStarColor(ContextCompat.getColor(this, R.color.ratingGrey))
            }
        }

        userId = intent.getStringExtra("uid")
        reference = intent.getStringExtra("ref")

        titleEditText = findViewById(R.id.title_field)
        descEditText = findViewById(R.id.description_field)
        priorityBar = findViewById(R.id.priority_bar)
        categorySpinner = findViewById(R.id.category_spinner)
        completeCheckBox = findViewById(R.id.complete_checkbox)
        addEditButton = findViewById(R.id.add_edit_button)

        firebaseDatabase = FirebaseDatabase.getInstance()

        if (reference != null) {
            addEditButton.setText("Update")
            databaseReference = firebaseDatabase.getReference("to_do").child(reference!!)
            val valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listItem = snapshot.getValue(ListItem::class.java)
                    setUi()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("DetailActivity", error.toString())
                }

            }
            databaseReference.addValueEventListener(valueEventListener)

            addEditButton.setOnClickListener {
                //edit database item
                getValuesFromUi()
                databaseReference.setValue(listItem)
                finish()
                finish()
            }
        } else {
            addEditButton.setOnClickListener{
                getValuesFromUi()

                //add to database
                databaseReference = firebaseDatabase.getReference("to_do")
                databaseReference.push().setValue(listItem)

                finish()
            }
        }
    }

    private fun RatingBar.setStarColor(color: Int) {
        val progressDrawable = progressDrawable as LayerDrawable
        val stars = progressDrawable.getDrawable(2) as Drawable
        stars.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        progressDrawable.setDrawableByLayerId(android.R.id.progress, stars)
    }

    private fun getValuesFromUi(){
        //update object properties from UI
        if (listItem!= null) {
            listItem!!.uid = userId ?: ""
            listItem!!.title = titleEditText.text.toString()
            listItem!!.description = descEditText.text.toString()
            listItem!!.category = categorySpinner.selectedItem.toString()
            listItem!!.priority = priorityBar.rating.toInt()
            listItem!!.complete = completeCheckBox.isChecked

        }

    }

    // function that returns position of category from the array
    private fun getCategoryPosition(category: String): Int {
        // Helper function to get the position of a category in the spinner
        val categories = resources.getStringArray(R.array.product_categories)
        return categories.indexOf(category)
    }

    private fun setUi(){
        // set components to display detail information
        if(listItem != null) {
            titleEditText.setText(listItem!!.title)
            descEditText.setText(listItem!!.description)
            categorySpinner.setSelection(getCategoryPosition(listItem!!.category))
            priorityBar.rating = listItem!!.priority.toFloat()
            completeCheckBox.isChecked = listItem!!.complete
        }
        else{
            titleEditText.setText("")
            categorySpinner.setSelection(0)
            completeCheckBox.isChecked = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.delete_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == R.id.delete) {
            //delete item
            databaseReference.removeValue()
            finish()
            true
        } else return if (item.itemId == R.id.back_item) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
        else super.onOptionsItemSelected(item)
    }
}