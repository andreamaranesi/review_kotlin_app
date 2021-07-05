package com.project.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.project.review.databinding.MainActivityBinding
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import com.project.review.ui.HomeDirections
import com.project.review.ui.Results
import com.project.review.ui.ResultsDirections
import com.project.review.ui.ScannerActivity
import com.project.review.view_models.ReviewViewModel


/**
 * contains the NavHostFragment
 * hosts Home, Results and Filters fragments
 *
 * @see com.project.review.ui.Home
 * @see com.project.review.ui.Results
 * @see com.project.review.ui.filters.Filters
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    Results.Actions {
    private lateinit var navController: NavController
    private lateinit var context: MainActivity
    private lateinit var actionBar: ActionBar

    val reviewModel: ReviewViewModel by viewModels()
    private lateinit var linearLayout: LinearLayoutManager


    private var filterMenu: MenuItem? = null
    private var filterSearch: MenuItem? = null


    /**
     * checks if we are on Home
     */
    private fun isHome(): Boolean {
        if (this.navController.currentDestination?.id == R.id.home) {
            return true
        }
        return false
    }

    /**
     * checks if we are on Results
     */
    private fun isResults(): Boolean {
        if (this.navController.currentDestination?.id == R.id.results) {
            return true
        }
        return false
    }

    lateinit var binding: MainActivityBinding
    private lateinit var originalLayout: ViewGroup.LayoutParams

    private fun setDrawer(enabled: Boolean) {
        val lockMode =
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        binding.drawerLayout.setDrawerLockMode(lockMode)

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.context = this

        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)

        onCameraClick()
        val editText: EditText = binding.searchEditText


        binding.search.setOnClickListener {
            val research = editText.text.toString()
            if (research.isNotEmpty()) {
                Tools.searchReviews(reviewModel, research, isSpecificCode = false)
                navController.navigate(HomeDirections.actionHomeToResults())
            }
        }

        originalLayout = binding.searchBarContainer.layoutParams

        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).findNavController()

        val extras = intent.extras
        if (extras?.isEmpty == false)
            this.searchItem(extras.getString(Settings.CODE, ""), true)

        initMenu()
        listenDestination()



        editText.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
                if (p1 == EditorInfo.IME_ACTION_SEARCH) {

                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(p0?.windowToken, 0)

                    searchItem(p0?.text.toString(), false)

                    return true
                }
                return false
            }

        })

        checkInternetConnection()

    }

    var snackBar: Snackbar? = null;

    /**
     * if, during a search, the device loses connection to the network, a SnackBar will appear
     */
    private fun checkInternetConnection() {
        Settings.networkAvailable.observe(this) {
            snackBar?.dismiss()
            if (isResults() && !it) {
                snackBar = Snackbar.make(binding.coordinator,
                    getString(R.string.connestion_lost),
                    Snackbar.LENGTH_LONG)
                snackBar?.setAction(getString(R.string.close)) {
                    snackBar?.dismiss()
                }
                snackBar?.show()
            }
        }
    }

    /**
     * calls the Activity ScannerActivity
     *
     * @see ScannerActivity
     */
    private fun onCameraClick() {
        binding.camera.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            this.startActivityForResult(intent, 1)

        }
    }

    private var currentProductVisibility = false

    private fun setMenuItemVisibility() {
        val visibility = isResults() && currentProductVisibility
        filterMenu?.isVisible = visibility
        filterSearch?.isVisible = visibility
    }


    /**
     * checks the changes to be made to the ui when navigating from one fragment to another
     */
    private fun listenDestination() {
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            setMenuItemVisibility()

            if (isHome()) {
                this.setDrawer(true)
            } else
                this.setDrawer(false)

            if (!isHome()) {

                binding.searchBarContainer.layoutParams =
                    AppBarLayout.LayoutParams(
                        android.app.ActionBar.LayoutParams.MATCH_PARENT,
                        android.app.ActionBar.LayoutParams.WRAP_CONTENT
                    ).apply {
                        scrollFlags = 0
                    }

                binding.searchBar.visibility = View.GONE

            } else {
                binding.searchBarContainer.layoutParams = this.originalLayout
                binding.searchBar.visibility = View.VISIBLE
            }

        }
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun initMenu() {

        //CHANGES THE APPBAR TEXT WHEN RESULTS ARE FOUND DURING A SEARCH
        reviewModel._filteredItemCounter.observe(this, {
            if (isResults())
                supportActionBar?.title = "$it " + getString(R.string.reviews)
        })

        /* UNTIL A PRODUCT IS NOT FOUND DURING A SEARCH, DENY THE POSSIBILITY OF
        NAVIGATING TO Results
        */
        reviewModel.currentProduct.observe(this, {
            currentProductVisibility = it != null
            this.setMenuItemVisibility()
        })

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.home), binding.drawerLayout
        )
        setSupportActionBar(binding.toolbar)
        actionBar = supportActionBar!!

        val navigationView = binding.navView
        navigationView.setNavigationItemSelectedListener(this)
        setupActionBarWithNavController(
            navController, appBarConfiguration
        )

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_view -> {
                navController.navigate(ResultsDirections.actionResultsToFilters())
                return false
            }
        }

        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    /**
     * filterMenu and filterSearch are obtained from here
     *
     * @see filterMenu
     * @see filterSearch
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        filterMenu = menu?.findItem(R.id.filter_view)
        filterSearch = menu?.findItem(R.id.filter_search)

        filterMenu?.isVisible = false
        setUpFilterSearchArea()
        return true
    }

    /**
     * creates methods for capturing text to filter reviews
     */
    private fun setUpFilterSearchArea() {
        val searchView: androidx.appcompat.widget.SearchView =
            filterSearch?.actionView as androidx.appcompat.widget.SearchView


        val searchSrcText =
            searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text);

        searchSrcText.hint = getString(R.string.search_reviews)
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (p0 != null) {
                    linearLayout.scrollToPosition(0)
                    reviewModel.setSearchText(p0)
                }
                return true
            }

        })
    }


    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_github -> {
                val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Settings.GITHUB_URL))
                startActivity(openUrlIntent)
            }
        }

        closeDrawer()
        return true
    }

    private fun searchItem(
        code: String,
        isBarCode: Boolean,
        max: Int = Settings.reviewResults,
        type: Int = 0,
    ) {
        if (code.isNotEmpty()) {

            if (isHome())
                navController.navigate(
                    HomeDirections.actionHomeToResults()
                )

            Tools.searchReviews(reviewModel, code, isBarCode, max, type)
        }
    }

    /**
     * when the barcode is read
     *
     * @see ScannerActivity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val code = data?.getStringExtra(Settings.CODE) ?: ""
            // val format: Int = data?.getIntExtra(Settings.BARCODE_FORMAT, 0) ?: 0
            searchItem(code, true)
        }
    }

    /**
     * gets the LinearLayoutManager of RecyclerView of the fragment Results
     *
     * @see Results.Actions
     */
    override fun setLinearLayout(linearLayoutManager: LinearLayoutManager) {
        this.linearLayout = linearLayoutManager
    }


}