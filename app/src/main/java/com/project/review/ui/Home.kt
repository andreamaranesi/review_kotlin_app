package com.project.review.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.review.R
import com.project.review.databinding.HomeBinding
import com.project.review.models.Product
import com.project.review.models.RelatedProduct
import com.project.review.view_models.HomeViewModel
import com.project.review.view_models.ReviewViewModel
import com.project.review.settings.Settings
import com.project.review.settings.Tools
import com.project.review.ui.recyclerview_adapters.ProductSavedReviewAdapter
import com.project.review.ui.recyclerview_adapters.RecentSearchAdapter
import com.project.review.ui.recyclerview_adapters.RelatedProductAdapter


class Home : Fragment(), ProductSavedReviewAdapter.Actions,
    RelatedProductAdapter.Actions, RecentSearchAdapter.Actions {
    private val viewModel: HomeViewModel by viewModels()
    private val reviewModel: ReviewViewModel by activityViewModels()


    private lateinit var binding: HomeBinding


    private lateinit var savedReviewsAdapter: ProductSavedReviewAdapter
    private lateinit var relatedProductAdapter: RelatedProductAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedReviewsAdapter = ProductSavedReviewAdapter(this)
        relatedProductAdapter = RelatedProductAdapter(
            this
        )
        recentSearchAdapter = RecentSearchAdapter(this)

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.home, container, false)
        binding = HomeBinding.bind(view)
        return view
    }


    /**
     * si istanziano le varie sezioni, andando ad ascoltare opportuni LiveData
     *
     * @see com.project.review.view_models.HomeViewModel
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //1° SEZIONE
        viewModel.getResearches(Settings.minListResults)
            .observe(viewLifecycleOwner, {
                recentSearchAdapter.submitList(it)
                binding.recentResearchesRecyclerview.visibility =
                    if (it.isEmpty()) View.GONE else View.VISIBLE
                binding.recentResearchesViewAll.visibility =
                    if (it.isEmpty() || it.size < Settings.minListResults) View.GONE else View.VISIBLE
                binding.recentResearchNoResults.visibility =
                    if (it.isEmpty()) View.VISIBLE else View.GONE
            })

        //2° SEZIONE
        viewModel.getProductWithReviews(Settings.minListResults)
            .observe(viewLifecycleOwner, {
                savedReviewsAdapter.submitList(it)
                binding.savedReviewsRecyclerview.visibility =
                    if (it.isEmpty()) View.GONE else View.VISIBLE
                binding.savedReviewsViewAll.visibility =
                    if (it.isEmpty() || it.size < Settings.minListResults) View.GONE else View.VISIBLE
                binding.reviewsNoResults.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            })

        //3° SEZIONE
        viewModel.limitedRelatedProducts.observe(viewLifecycleOwner, {
            relatedProductAdapter.submitList(it)
            binding.relatedProductsViewAll.visibility =
                if (it.isEmpty() || it.size < Settings.minListResults) View.VISIBLE else View.VISIBLE
            binding.relatedProducts.visibility =
                if (it.isEmpty()) View.GONE else View.VISIBLE
        })

        this.setRecentResearches()
        this.setRelatedProducts()
        this.setProductWithSavedReviews()

    }

    /**
     * interrompe l'eventuale ricerca in background di recensioni
     */
    override fun onResume() {
        super.onResume()
        reviewModel.forceStop()
        Log.i("Home", "Research Possibly Interrupted")
    }

    private fun setProductWithSavedReviews() {
        val recyclerView = binding.savedReviewsRecyclerview
        recyclerView.setHasFixedSize(false)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = savedReviewsAdapter
        binding.savedReviewsViewAll.setOnClickListener {
            startActivity(Intent(context, ProductSavedReviewActivity::class.java))
        }
    }

    private val relatedProductLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val code = extras?.getString(Settings.CODE, "")
                if (code?.isNotEmpty() == true) {
                    this.searchRelatedProduct(code)
                }
            }
        }


    private fun setRelatedProducts() {
        val recyclerView = binding.relatedProductsRecyclerview
        recyclerView.setHasFixedSize(false)
        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = relatedProductAdapter
        binding.relatedProductsViewAll.setOnClickListener {
            relatedProductLauncher.launch(
                Intent(
                    context,
                    RelatedProductActivity::class.java
                )
            )
        }

    }

    private val recentResearchLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val code = extras?.getString(Settings.CODE, "")
                if (code?.isNotEmpty() == true) {
                    this.searchProduct(code)
                }
            }
        }


    private fun setRecentResearches() {
        val recyclerView = binding.recentResearchesRecyclerview
        recyclerView.setHasFixedSize(false)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recentSearchAdapter

        binding.recentResearchesViewAll.setOnClickListener {
            recentResearchLauncher.launch(
                Intent(
                    context,
                    RecentResearchActivity::class.java
                ))
        }

    }


    private fun searchRelatedProduct(code: String) {
        Tools.searchReviews(reviewModel, code, false, isAsin = true)
        findNavController().navigate(HomeDirections.actionHomeToResults())
    }

    override fun onRelatedProductClick(product: RelatedProduct) {
        this.searchRelatedProduct(product.code)
    }

    private fun searchProduct(code: String) {
        Tools.searchReviews(reviewModel, code, true, isAsin = false)
        findNavController().navigate(HomeDirections.actionHomeToResults())
    }

    override fun onRecentResearchClick(product: Product, value: Boolean) {
        this.searchProduct(product.code)
    }

    override fun onLongRecentResearchClick(product: Product) {
        //NON IMPLEMENTATO NEL FRAGMENT CORRENTE
    }

    override fun onProductWithReviewsClick(product: Product) {
        startActivity(Intent(context, SavedReviewActivity::class.java).apply {
            putExtra(Settings.CODE, product.code)
            putExtra(Settings.PRODUCT_NAME, product.name)
        })
    }

}