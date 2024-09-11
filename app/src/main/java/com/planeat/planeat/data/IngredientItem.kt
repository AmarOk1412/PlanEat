package com.planeat.planeat.data

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.planeat.planeat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.tensorflow.lite.examples.textclassification.client.IngredientClassifier

@Serializable
data class ParsedIngredient(
    val name: String? = null,
    val qty: String? = null,
    val unit: String? = null
)

@Serializable
class IngredientItem(var name: String = "", var quantity: Float = 1.0f, var unit: String = "", var category: String = "") {

    fun addQuantity(other: IngredientItem) {
        if ((this.unit == other.unit)
            || (this.unit == "piece" && other.unit == "clove")
            || (this.unit == "clove" && other.unit == "piece")
        ) {
            this.quantity += other.quantity
        } else {
            // Handle unit conversion or throw an error
            Log.e("PlanEat", "@@@ TODO convert ${other.unit} to ${this.unit}")
        }
    }
}

@Composable
fun toIngredientIcon(ingredientName: String): Painter? {
    // Store the icon resource ID
    var iconResId by remember { mutableStateOf<Int?>(null) }
    val db = IngredientsDb.getDatabase(LocalContext.current)

    // Fetch the icon resource ID in a background thread
    LaunchedEffect(ingredientName) {
        withContext(Dispatchers.IO) {
            val ingredient = db.ingredientDao().findByName(ingredientName)
            if (ingredient != null) {
                iconResId = ingredient.icon
            } else {
                val res = fetchIconForIngredient(ingredientName.lowercase())
                if (res != 0) {
                    iconResId = res
                    val ingredient = Ingredient(name=ingredientName, icon=iconResId!!)
                    try {
                        db.ingredientDao().insertAll(ingredient)
                    } catch (error: Exception) {
                        Log.w("PlanEat", "Error: $error")
                    }
                } else {
                    iconResId = R.drawable.bagel_3d
                }
            }
        }
    }

    // Return the ImageVector in the composable context
    return iconResId?.let { painterResource(id=it) }
}

suspend fun toIngredientCategory(
    ingredientName: String,
    ic: IngredientClassifier,
    db: IngredientsDb
): String {
    var category: String? = null

    withContext(Dispatchers.IO) {
        val ingredient = db.ingredientDao().findByName(ingredientName)
        if (ingredient != null && ingredient.category.isNotEmpty()) {
            category = ingredient.category

            Log.d("PlanEat", "@@@ => ${ingredient.category}")
        } else if (ingredient != null) {
            category = ic.classify(ingredientName.lowercase())

            try {
                ingredient.category = category!!
                db.ingredientDao().update(ingredient)
            } catch (error: Exception) {
                Log.w("PlanEat", "Error: $error")
            }
        } else {
            category = ""
        }
    }

    return category ?: ""
}

fun fetchIconForIngredient(ingredient_name: String): Int? {
    if (ingredient_name.contains("amphora")) {
        return R.drawable.amphora_3d
    } else if (ingredient_name.contains("avocado")) {
        return R.drawable.avocado_3d
    } else if (ingredient_name.contains("baby")) {
        return R.drawable.baby_bottle_3d
    }
    else if (ingredient_name.contains("bacon")) {
        return R.drawable.bacon_3d
    }
    else if (ingredient_name.contains("bagel")) {
        return R.drawable.bagel_3d
    }
    else if (ingredient_name.contains("bread")) {
        return R.drawable.baguette_bread_3d
    }
    else if (ingredient_name.contains("water")) {
        return R.drawable.water_3d
    }
    else if (ingredient_name.contains("banana")) {
        return R.drawable.banana_3d
    }
    else if (ingredient_name.contains("beans")) {
        return R.drawable.beans_3d
    }
    else if (ingredient_name.contains("beer")) {
        return R.drawable.beer_mug_3d
    }
    else if (ingredient_name.contains("bell pepper")) {
        return R.drawable.bell_pepper_3d
    }
    else if (ingredient_name.contains("bento_box")) {
        return R.drawable.bento_box_3d
    }
    else if (ingredient_name.contains("beverage")) {
        return R.drawable.beverage_box_3d
    }
    else if (ingredient_name.contains("cake")) {
        return R.drawable.birthday_cake_3d
    }
    else if (ingredient_name.contains("blueberries")) {
        return R.drawable.blueberries_3d
    }
    else if (ingredient_name.contains("bottle")) {
        return R.drawable.bottle_with_popping_cork_3d
    }
    else if (ingredient_name.contains("bowl")) {
        return R.drawable.bowl_with_spoon_3d
    }
    else if (ingredient_name.contains("bread")) {
        return R.drawable.bread_3d
    }
    else if (ingredient_name.contains("broccoli")) {
        return R.drawable.broccoli_3d
    }
    else if (ingredient_name.contains("bubble tea")) {
        return R.drawable.bubble_tea_3d
    }
    else if (ingredient_name.contains("burrito")) {
        return R.drawable.burrito_3d
    }
    else if (ingredient_name.contains("butter")) {
        return R.drawable.butter_3d
    }
    else if (ingredient_name.contains("candy")) {
        return R.drawable.candy_3d
    }
    else if (ingredient_name.contains("can")) {
        return R.drawable.canned_food_3d
    }
    else if (ingredient_name.contains("carrot")) {
        return R.drawable.carrot_3d
    }
    else if (ingredient_name.contains("cheese")) {
        return R.drawable.cheese_wedge_3d
    }
    else if (ingredient_name.contains("cherries")) {
        return R.drawable.cherries_3d
    }
    else if (ingredient_name.contains("chestnut")) {
        return R.drawable.chestnut_3d
    }
    else if (ingredient_name.contains("chocolate")) {
        return R.drawable.chocolate_bar_3d
    }
    else if (ingredient_name.contains("chopsticks")) {
        return R.drawable.chopsticks_3d
    }
    else if (ingredient_name.contains("clinking_beer_mugs")) {
        return R.drawable.clinking_beer_mugs_3d
    }
    else if (ingredient_name.contains("clinking_glasses")) {
        return R.drawable.clinking_glasses_3d
    }
    else if (ingredient_name.contains("cocktail")) {
        return R.drawable.cocktail_glass_3d
    }
    else if (ingredient_name.contains("coconut")) {
        return R.drawable.coconut_3d
    }
    else if (ingredient_name.contains("rice")) {
        return R.drawable.cooked_rice_3d
    }
    else if (ingredient_name.contains("cookie")) {
        return R.drawable.cookie_3d
    }
    else if (ingredient_name.contains("cooking")) {
        return R.drawable.cooking_3d
    }
    else if (ingredient_name.contains("crab")) {
        return R.drawable.crab_3d
    }
    else if (ingredient_name.contains("croissant")) {
        return R.drawable.croissant_3d
    }
    else if (ingredient_name.contains("cucumber")) {
        return R.drawable.cucumber_3d
    }
    else if (ingredient_name.contains("cupcake")) {
        return R.drawable.cupcake_3d
    }
    else if (ingredient_name.contains("cup")) {
        return R.drawable.cup_with_straw_3d
    }
    else if (ingredient_name.contains("curry")) {
        return R.drawable.curry_rice_3d
    }
    else if (ingredient_name.contains("custard")) {
        return R.drawable.custard_3d
    }
    else if (ingredient_name.contains("meat")) {
        return R.drawable.cut_of_meat_3d
    }
    else if (ingredient_name.contains("dango")) {
        return R.drawable.dango_3d
    }
    else if (ingredient_name.contains("doughnut")) {
        return R.drawable.doughnut_3d
    }
    else if (ingredient_name.contains("dumpling")) {
        return R.drawable.dumpling_3d
    }
    else if (ingredient_name.contains("corn")) {
        return R.drawable.ear_of_corn_3d
    }
    else if (ingredient_name.contains("eggplant")) {
        return R.drawable.eggplant_3d
    }
    else if (ingredient_name.contains("egg")) {
        return R.drawable.egg_3d
    }
    else if (ingredient_name.contains("falafel")) {
        return R.drawable.falafel_3d
    }
    else if (ingredient_name.contains("fish")) {
        return R.drawable.fish_cake_with_swirl_3d
    }
    else if (ingredient_name.contains("flatbread")) {
        return R.drawable.flatbread_3d
    }
    else if (ingredient_name.contains("fondue")) {
        return R.drawable.fondue_3d
    }
    else if (ingredient_name.contains("fork_and_knife")) {
        return R.drawable.fork_and_knife_3d
    }
    else if (ingredient_name.contains("fortune cookie")) {
        return R.drawable.fortune_cookie_3d
    }
    else if (ingredient_name.contains("fries")) {
        return R.drawable.french_fries_3d
    }
    else if (ingredient_name.contains("shrimp")) {
        return R.drawable.fried_shrimp_3d
    }
    else if (ingredient_name.contains("garlic") || ingredient_name.contains("shallot")) {
        return R.drawable.garlic_3d
    }
    else if (ingredient_name.contains("milk")) {
        return R.drawable.glass_of_milk_3d
    }
    else if (ingredient_name.contains("grapes")) {
        return R.drawable.grapes_3d
    }
    else if (ingredient_name.contains("apple")) {
        return R.drawable.green_apple_3d
    }
    else if (ingredient_name.contains("salad")) {
        return R.drawable.green_salad_3d
    }
    else if (ingredient_name.contains("hamburger")) {
        return R.drawable.hamburger_3d
    }
    else if (ingredient_name.contains("honey")) {
        return R.drawable.honey_pot_3d
    }
    else if (ingredient_name.contains("beverage")) {
        return R.drawable.hot_beverage_3d
    }
    else if (ingredient_name.contains("hotdog")) {
        return R.drawable.hot_dog_3d
    }
    else if (ingredient_name.contains("hot pepper")) {
        return R.drawable.hot_pepper_3d
    }
    else if (ingredient_name.contains("ice")) {
        return R.drawable.ice_3d
    }
    else if (ingredient_name.contains("icecream")) {
        return R.drawable.ice_cream_3d
    }
    else if (ingredient_name.contains("jar")) {
        return R.drawable.jar_3d
    }
    else if (ingredient_name.contains("kitchen_knife")) {
        return R.drawable.kitchen_knife_3d
    }
    else if (ingredient_name.contains("kiwi")) {
        return R.drawable.kiwi_fruit_3d
    }
    else if (ingredient_name.contains("leafy")) {
        return R.drawable.leafy_green_3d
    }
    else if (ingredient_name.contains("lemon")) {
        return R.drawable.lemon_3d
    }
    else if (ingredient_name.contains("lobster")) {
        return R.drawable.lobster_3d
    }
    else if (ingredient_name.contains("lollipop")) {
        return R.drawable.lollipop_3d
    }
    else if (ingredient_name.contains("mango")) {
        return R.drawable.mango_3d
    }
    else if (ingredient_name.contains("mate")) {
        return R.drawable.mate_3d
    }
    else if (ingredient_name.contains("meat")) {
        return R.drawable.meat_on_bone_3d
    }
    else if (ingredient_name.contains("melon")) {
        return R.drawable.melon_3d
    }
    else if (ingredient_name.contains("cake")) {
        return R.drawable.moon_cake_3d
    }
    else if (ingredient_name.contains("mushroom")) {
        return R.drawable.mushroom_3d
    }
    else if (ingredient_name.contains("oden")) {
        return R.drawable.oden_3d
    }
    else if (ingredient_name.contains("olive")) {
        return R.drawable.olive_3d
    }
    else if (ingredient_name.contains("onion")) {
        return R.drawable.onion_3d
    }
    else if (ingredient_name.contains("oyster")) {
        return R.drawable.oyster_3d
    }
    else if (ingredient_name.contains("pancakes")) {
        return R.drawable.pancakes_3d
    }
    else if (ingredient_name.contains("peach")) {
        return R.drawable.peach_3d
    }
    else if (ingredient_name.contains("peanuts")) {
        return R.drawable.peanuts_3d
    }
    else if (ingredient_name.contains("pear")) {
        return R.drawable.pear_3d
    }
    else if (ingredient_name.contains("pie")) {
        return R.drawable.pie_3d
    }
    else if (ingredient_name.contains("pineapple")) {
        return R.drawable.pineapple_3d
    }
    else if (ingredient_name.contains("pizza")) {
        return R.drawable.pizza_3d
    }
    else if (ingredient_name.contains("popcorn")) {
        return R.drawable.popcorn_3d
    }
    else if (ingredient_name.contains("potato")) {
        return R.drawable.potato_3d
    }
    else if (ingredient_name.contains("food")) {
        return R.drawable.pot_of_food_3d
    }
    else if (ingredient_name.contains("poultry")) {
        return R.drawable.poultry_leg_3d
    }
    else if (ingredient_name.contains("liquid")) {
        return R.drawable.pouring_liquid_3d
    }
    else if (ingredient_name.contains("pretzel")) {
        return R.drawable.pretzel_3d
    }
    else if (ingredient_name.contains("red apple")) {
        return R.drawable.red_apple_3d
    }
    else if (ingredient_name.contains("rice ball")) {
        return R.drawable.rice_ball_3d
    }
    else if (ingredient_name.contains("rice cracker")) {
        return R.drawable.rice_cracker_3d
    }
    else if (ingredient_name.contains("sweet potato")) {
        return R.drawable.roasted_sweet_potato_3d
    }
    else if (ingredient_name.contains("sake")) {
        return R.drawable.sake_3d
    }
    else if (ingredient_name.contains("salt")) {
        return R.drawable.salt_3d
    }
    else if (ingredient_name.contains("sandwich")) {
        return R.drawable.sandwich_3d
    }
    else if (ingredient_name.contains("shallow_pan_of_food")) {
        return R.drawable.shallow_pan_of_food_3d
    }
    else if (ingredient_name.contains("shaved_ice")) {
        return R.drawable.shaved_ice_3d
    }
    else if (ingredient_name.contains("shortcake")) {
        return R.drawable.shortcake_3d
    }
    else if (ingredient_name.contains("shrimp")) {
        return R.drawable.shrimp_3d
    }
    else if (ingredient_name.contains("soft_ice_cream")) {
        return R.drawable.soft_ice_cream_3d
    }
    else if (ingredient_name.contains("spaghetti") || ingredient_name.contains("pasta") || ingredient_name.contains("noodle")) {
        return R.drawable.spaghetti_3d
    }
    else if (ingredient_name.contains("spoon")) {
        return R.drawable.spoon_3d
    }
    else if (ingredient_name.contains("squid")) {
        return R.drawable.squid_3d
    }
    else if (ingredient_name.contains("steaming_bowl")) {
        return R.drawable.steaming_bowl_3d
    }
    else if (ingredient_name.contains("strawberry")) {
        return R.drawable.strawberry_3d
    }
    else if (ingredient_name.contains("stuffed_flatbread")) {
        return R.drawable.stuffed_flatbread_3d
    }
    else if (ingredient_name.contains("sushi")) {
        return R.drawable.sushi_3d
    }
    else if (ingredient_name.contains("taco")) {
        return R.drawable.taco_3d
    }
    else if (ingredient_name.contains("takeout_box")) {
        return R.drawable.takeout_box_3d
    }
    else if (ingredient_name.contains("tamale")) {
        return R.drawable.tamale_3d
    }
    else if (ingredient_name.contains("tangerine")) {
        return R.drawable.tangerine_3d
    }
    else if (ingredient_name.contains("tea")) {
        return R.drawable.teacup_without_handle_3d
    }
    else if (ingredient_name.contains("teapot")) {
        return R.drawable.teapot_3d
    }
    else if (ingredient_name.contains("tomato")) {
        return R.drawable.tomato_3d
    }
    else if (ingredient_name.contains("drink")) {
        return R.drawable.tropical_drink_3d
    }
    else if (ingredient_name.contains("tumbler_glass")) {
        return R.drawable.tumbler_glass_3d
    }
    else if (ingredient_name.contains("waffle")) {
        return R.drawable.waffle_3d
    }
    else if (ingredient_name.contains("watermelon")) {
        return R.drawable.watermelon_3d
    }
    else if (ingredient_name.contains("wine_glass")) {
        return R.drawable.wine_glass_3d
    }
    else if (ingredient_name.contains("chicken")) {
        return R.drawable.chicken_3d
    }
    else if (ingredient_name.contains("beef")) {
        return R.drawable.beef_3d
    }
    else if (ingredient_name.contains("duck")) {
        return R.drawable.duck_3d
    }
    else if (ingredient_name.contains("lamb")) {
        return R.drawable.lamb_3d
    }
    else if (ingredient_name.contains("pork")) {
        return R.drawable.pork_3d
    }
    else if (ingredient_name.contains("rabbit")) {
        return R.drawable.rabbit_3d
    }

    return 0
}