package com.project.review

import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.project.review.settings.Settings
import com.schibsted.spain.barista.interaction.BaristaScrollInteractions.scrollTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReviewInstrumentTest {

    companion object {


        fun getText(matcher: ViewInteraction): String {
            var text = String()
            matcher.perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return isAssignableFrom(TextView::class.java)
                }

                override fun getDescription(): String {
                    return "Text of the view"
                }

                override fun perform(uiController: UiController, view: View) {
                    val tv = view as TextView
                    text = tv.text.toString()
                }
            })

            return text
        }
    }

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)


    @Test
    fun changeText_sameActivity() {

        onView(withId(R.id.search_edit_text))
            .perform(typeText("iphone 11"), closeSoftKeyboard())
        onView(withId(R.id.search)).perform(click())

        var index = 0
        var result = false
        activityRule.scenario.onActivity { activity ->
            activity.reviewModel._reviewListener.observe(activity, {
                if (activity.reviewModel.currentFilteredReviews() > 0) {
                    result = true
                    index = 1
                }
                if (activity.reviewModel.noResults())
                    index = 1

            })
        }


        // BUSY WAITING until first results are found
        while (index == 0) {
        }

        if (result) {
            onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                    click()))
            onView(withId(R.id.wishlist_button)).perform(click())
            val reviewBody = getText(onView(withId(R.id.review_body)))
            Log.i("ReviewInstrumentTest", reviewBody)
            onView(isRoot()).perform(pressBack())
            onView(isRoot()).perform(pressBack())
            scrollTo(R.id.saved_reviews_recyclerview)
            onView(withId(R.id.saved_reviews_recyclerview))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                    click()))
            onView(withId(R.id.items_full_recyclerview))
                .perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    object : Matcher<View> {
                        override fun describeTo(description: Description?) {

                        }

                        override fun matches(item: Any?): Boolean {
                            if (item != null && item is View) {
                                var text =
                                    item.findViewById<TextView>(R.id.review_body).text.toString()

                                if (text.length >= Settings.maxBodyLength - 4)
                                    text = text.substring(0, text.length - 4)


                                return reviewBody.contains(text)
                            }
                            return false
                        }

                        override fun describeMismatch(
                            item: Any?,
                            mismatchDescription: Description?,
                        ) {
                            TODO("Not yet implemented")
                        }

                        override fun _dont_implement_Matcher___instead_extend_BaseMatcher_() {
                            TODO("Not yet implemented")
                        }

                    }, click()))
        } else
            assert(false)
    }

}