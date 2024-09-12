import pandas as pd
import random

from sklearn.model_selection import train_test_split

# Define categories
categories = {
    "Alcool": [
        "Beer", "Wine", "Vodka", "Whiskey", "Rum", "Tequila", "Champagne", "Liqueur", "Brandy", "Cider",
        "Gin", "Port", "Sherry", "Sake", "Absinthe", "Mead", "Pilsner", "Stout", "Ale", "Lager",
        "Sour Beer", "Barleywine", "IPA", "Craft Beer", "Amaretto", "Calvados", "Pisco", "Triple Sec", "Bourbon",
        "Tequila Sunrise", "Mai Tai", "Margarita", "Martini", "Negroni", "Old Fashioned", "Moscow Mule", "Manhattan", "Bloody Mary", "Sazerac",
        "Mint Julep", "Daiquiri", "Sangria", "Bellini", "Sidecar", "Tom Collins", "Pina Colada", "Rum Punch", "Cosmopolitan", "Whiskey Sour",
        "Rum Runner", "Mai Tai", "Zombie", "Long Island Iced Tea", "Aperol Spritz", "Mimosa", "Dark 'n' Stormy", "Caipirinha", "Gimlet", "Rob Roy",
        "Amaretto Sour", "Brandy Alexander", "Clover Club", "Corpse Reviver #2", "French 75", "Ginger Beer", "Pisco Sour", "Ramos Gin Fizz", "Mai Tai", "Sazerac",
        "Vesper Martini", "Boulevardier", "Tommy's Margarita", "Pimm's Cup", "Clover Club", "Aviation", "Boulevardier", "Paloma", "Rum Punch", "Ginger Ale",
        "Chardonnay", "Pinot Noir", "Merlot", "Cabernet Sauvignon", "Zinfandel", "Riesling", "Sauvignon Blanc", "Syrah", "Malbec", "Chianti", "red wine", "white wine", "Jura white wine"
    ],
    "Soda": [
        "Cola", "Lemonade", "Orange Soda", "Root Beer", "Ginger Ale", "Club Soda", "Tonic Water", "Cream Soda", "Diet Soda", "Sparkling Water",
        "Cherry Soda", "Grape Soda", "Lime Soda", "Energy Drink", "Root Beer Float", "Fruit Punch", "Iced Tea", "Dr. Pepper", "Mountain Dew", "Pepsi",
        "Sprite", "7-Up", "Fanta", "Sunkist", "A&W Root Beer", "Barq's Root Beer", "Mug Root Beer", "Mello Yello", "Pibbs", "Squirt",
        "Red Bull", "Monster Energy", "Rockstar", "Gatorade", "Powerade", "Vitaminwater", "Snapple", "Ice Tea", "Hires Root Beer", "Diet Coke", "Sierra Mist",
        "Bubly", "LaCroix", "Zevia", "Spindrift", "Polar Seltzer", "Canada Dry", "Peach Soda", "Mango Soda", "Tamarind Soda", "Jamaican Ginger Beer",
        "Cranberry Soda", "Black Cherry Soda", "Key Lime Soda", "Diet Dr. Pepper", "Diet Mountain Dew", "Cherry Vanilla Coke", "Cherry Pepsi", "Mango Pepsi", "Orange Vanilla Coke",  "LaCroix", "Raspberry Lemonade", "Lemon Lime Gatorade", "Orange Gatorade", "liquid"
    ],
    "Baked Good": [
        "Bread", "Muffins", "Cookies", "Cake", "Brownies", "Bagels", "Donuts", "Croissants", "Pie", "Pastries",
        "Scones", "Cupcakes", "Tarts", "Eclairs", "Pancakes", "Waffles", "Cinnamon Rolls", "Danish", "Puff Pastry", "Brioche",
        "Shortbread", "Madeleines", "Babka", "Zucchini Bread", "Cornbread", "Focaccia", "Ciabatta", "Pretzels", "Baguette", "Rugelach",
        "Baklava", "Strudel", "Almond Cake", "Victoria Sponge", "Gingerbread", "Pumpkin Bread", "Chocolate Chip Cookies", "Snickerdoodles", "Peanut Butter Cookies", "Oatmeal Cookies",
        "Molasses Cookies", "Lemon Bars", "Whoopie Pies", "Lemon Cake", "Red Velvet Cake", "Carrot Cake", "German Chocolate Cake", "Angel Food Cake", "Bundt Cake", "Cake Pops",
        "Eclairs", "Profiteroles", "Madeleine", "Apple Pie", "Cherry Pie", "Pecan Pie", "Peach Cobbler", "Berry Crisp", "Rhubarb Pie", "Key Lime Pie",
        "Cheesecake", "Tiramisu", "Mousse", "Pavlova", "Fruit Tart", "Creme Brulee", "Panna Cotta", "Cannoli", "Raspberry Bars", "Butter Tarts"
    ],
    "Frozen": [
        "Ice Cream", "Frozen Pizza", "Frozen Vegetables", "Frozen Fruit", "Frozen Dinners", "Frozen Chicken", "Frozen Fish", "Frozen Potatoes", "Frozen Burritos", "Frozen Waffles",
        "Frozen Yogurt", "Frozen Meatballs", "Frozen Dumplings", "Frozen Ravioli", "Frozen Fries", "Frozen Burgers", "Frozen Onion Rings", "Frozen Pretzels", "Frozen Smoothie Packs", "Frozen Lasagna",
        "Frozen Mac and Cheese", "Frozen Breakfast Sandwiches", "Frozen Enchiladas", "Frozen Fried Rice", "Frozen Bagels", "Frozen Pasta", "Frozen Casserole", "Frozen Pancakes", "Frozen Pastries", "Frozen Berries",
        "Frozen Shrimp", "Frozen Sausages", "Frozen Hash Browns", "Frozen Tater Tots", "Frozen Sweet Corn", "Frozen Broccoli", "Frozen Cauliflower", "Frozen Green Beans", "Frozen Peas", "Frozen Mushrooms",
        "Frozen Pies", "Frozen Pot Pie", "Frozen Stuffed Peppers", "Frozen Chicken Wings", "Frozen Drumsticks", "Frozen Pizza Rolls", "Frozen Cheesesteaks", "Frozen Pizza Bagels", "Frozen Calzones", "Frozen Ribs",
        "Frozen Tofu", "Frozen Meatloaf", "Frozen Salmon Fillets", "Frozen Scallops", "Frozen Fish Sticks", "Frozen Crab Cakes", "Frozen Roasted Vegetables", "Frozen Mashed Potatoes", "Frozen Alfredo Sauce", "Frozen Meat Sauce"
    ],
    "Pantry": [
        "Canned Beans", "Pasta", "Rice", "Canned Tomatoes", "Peanut Butter", "Canned Soup", "Olive Oil", "Vinegar", "Dried Herbs", "Cereal",
        "Cooking Spray", "Sugar", "Flour", "Baking Powder", "Tomato Sauce", "Brown sugar", "White sugar",
        "Canned Vegetables", "Canned Fruit", "Canned Tuna", "Canned Chicken", "Dried Pasta", "Dried Lentils", "Canned Chili", "Evaporated Milk", "Condensed Milk", "Powdered Sugar", "dark chocolate", "no sugar chocolate", "chicken stock cubes",
        "Molasses", "Agave Syrup", "Soy Milk", "Coconut Milk", "Broth", "Stock Cubes", "Bouillon", "Cooking Wine", "Rice Vinegar", "Red Wine Vinegar",
        "Balsamic Vinegar", "Apple Cider Vinegar", "Worcestershire Sauce", "Barbecue Sauce", "Hot Sauce", "Mustard", "Mayonnaise", "Ranch Dressing", "Italian Dressing", "Pre-cooked ramen noodles", "ramen",
        "Teriyaki Sauce", "Hoisin Sauce", "Sriracha", "Dijon Mustard", "Miso Paste", "Pesto", "Tahini", "Coconut Aminos", "Nutritional Yeast", "Noodle Soup Mix",
        "Rice", "Oats", "Quinoa", "Barley", "Farro", "Couscous", "Bulgar", "Polenta", "Buckwheat",
        "Cornmeal", "Rye", "Amaranth", "Teff", "Millet", "Spelt", "Grits", "Freekeh", "Triticale", "Wild Rice",
        "Brown Rice", "White Rice", "Jasmine Rice", "Basmati Rice", "Arborio Rice", "Sushi Rice", "Instant Rice", "Black Rice", "Red Rice", "Forbidden Rice",
        "Quinoa Flakes", "Oat Bran", "Steel-Cut Oats", "Rolled Oats", "Cream of Wheat", "Polenta Flour", "Wheat Flour", "Rice Flour", "Corn Flour", "Almond Flour", "Coconut Flour", "Vanilla extract",
        "Sorghum", "Kamut", "Tapioca", "Arrowroot", "Chia Seeds", "Hemp Seeds", "Flax Seeds", "Psyllium Husk", "Nut Flours", "Soy Flour",
        "Spelt Flour", "Whole Wheat Flour", "Self-Raising Flour", "Pastry Flour", "Cake Flour", "Semolina", "Corn Starch", "Potato Flour", "Teff Flour"
    ],
    "Dried": [
        "Dried Fruit", "Dried Beans", "Dried Mushrooms", "Dried Seaweed", "Dried Tomatoes", "Dried Chilis", "Dried Apricots", "Dried Raisins", "Dried Cranberries", "Dried Peppers",
        "Dried Figs", "Dried Plums", "Dried Apples", "Dried Pears", "Dried Berries", "Dried Mango", "Dried Pineapple", "Dried Herbs", "Dried Garlic", "Dried Onions", "Dried Carrots",
        "Dried Zucchini", "Dried Mushrooms", "Dried Spinach", "Dried Kale", "Dried Celery", "Dried Leeks", "Dried Tomatoes", "Dried Shallots", "Dried Green Beans", "Dried Corn",
        "Dried Tomatoes", "Dried Eggplant", "Dried Broccoli", "Dried Chanterelles", "Dried Porcini", "Dried Shiitake", "Dried Morels", "Dried Lotus Root", "Dried Yams", "Dried Sweet Potatoes",
        "Dried Pineapple", "Dried Mango", "Dried Apples", "Dried Apricots", "Dried Figs", "Dried Pears", "Dried Raisins", "Dried Cranberries", "Dried Cherries", "Dried Blueberries",
        "Dried Banana Chips", "Dried Kiwi", "Dried Strawberries", "Dried Raspberries", "Dried Blackberries", "Dried Mulberries", "Dried Elderberries", "Dried Goji Berries", "Dried Dates", "Dried Dragon Fruit", "Dried Apples",
        "Dried Figs", "Dried Dates", "Dried Cherries", "Dried Apricots", "Dried Cranberries", "Dried Blueberries", "Dried Pears", "Dried Pineapple", "Dried Mango", "Dried Bananas"
    ],
    "Vegetables": [
        "Carrots", "Broccoli", "Spinach", "Bell Peppers", "Tomatoes", "Lettuce", "Onion", "Onions", "Garlic", "Cucumbers", "Zucchini",
        "Potatoes", "Sweet Potatoes", "Corn", "Green Beans", "Mushrooms", "Asparagus", "Brussels Sprouts", "Cauliflower", "Celery", "Radishes",
        "Beets", "Squash", "Pumpkin", "Artichokes", "Fennel", "Kale", "Swiss Chard", "Leeks", "Shallots", "Chili Peppers",
        "Okra", "Turnips", "Parsnips", "Daikon", "Kohlrabi", "Celery Root", "Endive", "Bok Choy", "Arugula", "Napa Cabbage",
        "Radicchio", "Rutabaga", "Jicama", "Dandelion Greens", "Seaweed", "Celeriac", "Sunchokes", "Yams", "Oregano", "Thyme",
        "Pumpkin", "Butternut Squash", "Acorn Squash", "Spaghetti Squash", "Chayote", "Taro", "Mizuna", "Watercress", "Snow Peas", "Snap Peas", "Hot pepper"
    ],
    "Fruits": [
        "Apples", "Bananas", "Oranges", "Strawberries", "Blueberries", "Pineapples", "Mangoes", "Peaches", "Grapes", "Pears",
        "Kiwis", "Plums", "Nectarines", "Apricots", "Raspberries", "Blackberries", "Cherries", "Melons", "Papayas", "Pomegranates",
        "Cantaloupe", "Honeydew", "Guava", "Dragon Fruit", "Passion Fruit", "Fig", "Date", "Lingonberry", "Gooseberry", "Starfruit",
        "Soursop", "Rhubarb", "Cranberries", "Tangerines", "Mandarins", "Blood Oranges", "Pawpaw", "Pomelo", "Elderberries", "Mulberries",
        "Nance", "Cupuacu", "Soursop", "Jackfruit", "Custard Apple", "Cherimoya", "Acerola", "Longan", "Raisins", "Mulberries", "Coconut",
    ],
    "Fresh": [
        "Butter", "Egg", "Milk", "Cream", "Eggs", "Orange juice", "Grenade juice", "Apple juice",
    ],
    "Fish": [
        "Salmon", "Tuna", "Cod", "Tilapia", "Trout", "Sardines", "Mackerel", "Herring", "Catfish", "Snapper",
        "Halibut", "Swordfish", "Bass", "Flounder", "Pollock", "Grouper", "Anchovies", "Redfish", "Eel", "Monkfish",
        "Yellowtail", "Lingcod", "Rockfish", "Barramundi", "Mahi Mahi", "Sole", "Pike", "Perch", "Carp", "Sturgeon",
        "Smelt", "Whitefish", "Crawfish", "Shrimp", "Crab", "Lobster", "Mussels", "Oysters", "Clams", "Scallops",
        "Abalone", "Octopus", "Squid", "Sardine", "Mackerel", "Mullet", "Swordfish", "Walleye", "Trout", "Rainbow Trout",
        "Bluegill", "Tilapia", "Channel Catfish", "Flathead Catfish", "American Shad", "Lake Trout", "Ocean Perch", "European Sea Bass", "Grouper", "Goldfish"
    ],
    "Condiments": [
        "Ketchup", "Mustard", "Mayonnaise", "Soy Sauce", "Hot Sauce", "BBQ Sauce", "Vinegar", "Relish", "Salsa", "Soy Sauce",
        "Chili Sauce", "Ranch Dressing", "Italian Dressing", "Tartar Sauce", "Buffalo Sauce", "Worcestershire Sauce", "Teriyaki Sauce", "Hoisin Sauce", "Sriracha", "Miso Paste", "Salt", "Pepper", "Soy Sauce", "Honey", "Maple Syrup",
        "Tahini", "Agave Syrup", "Molasses", "Soy Milk", "Rice Vinegar", "Red Wine Vinegar", "Balsamic Vinegar", "Apple Cider Vinegar", "Cooking Wine", "Dijon Mustard",
        "Whole Grain Mustard", "Yellow Mustard", "Hot English Mustard", "Sweet Pickle Relish", "Spicy Pickle Relish", "Cranberry Sauce", "Chimichurri", "Tzatziki", "Pesto", "Buffalo Sauce",
        "Honey Mustard", "Mango Chutney", "Plum Sauce", "Piri Piri Sauce", "Hollandaise Sauce", "Remoulade", "Aioli", "Tapenade", "Harissa", "Caper Sauce",
        "Demi-Glace", "Hollandaise", "Bearnaise", "Curry Sauce", "Cucumber Sauce", "Lemon Aioli", "Mole", "Katsu Sauce", "Lemon Butter Sauce", "Tomato Jam"
    ],
    "Meat": [
        "Beef", "Chicken", "Pork", "Lamb", "Turkey", "Bacon", "Sausage", "Ham", "Ground Beef", "Steak",
        "Chicken Thighs", "Chicken Breasts", "Pork Chops", "Pork Ribs", "Lamb Chops", "Ground Turkey", "Chicken Sausage", "Pork Belly", "Beef Brisket", "Beef Tenderloin", "Beef Ribs",
        "Veal", "Duck", "Goose", "Pastrami", "Corned Beef", "Pepperoni", "Salami", "Chorizo", "Kielbasa", "Andouille",
        "Prosciutto", "Capicola", "Mortadella", "Serrano Ham", "Bresaola", "Roast Beef", "Beef Short Ribs", "Beef Flank Steak", "Beef Chuck", "Lamb Shank",
        "Lamb Leg", "Lamb Shoulder", "Lamb Steaks", "Lamb Ribs", "Beef Oxtail", "Beef Chuck Roast", "Beef Filet", "Beef Top Round", "Pork Shoulder", "Pork Loin",
        "Pork Tenderloin", "Pork Ribs", "Pork Sausages", "Pork Chops", "Chicken Wings", "Chicken Drumsticks", "Chicken Liver", "Chicken Giblets", "Chicken Feet", "Duck Breast"
    ],
    "Cheese": [
        "Cheddar", "Mont d'or" "Mozzarella", "Parmesan", "Swiss", "Brie", "Gorgonzola", "Camembert", "Feta", "Goat Cheese", "Provolone",
        "Ricotta", "Mascarpone", "Pecorino", "Roquefort", "Gruyere", "American", "Colby", "Monterey Jack", "Havarti", "Asiago",
        "Blue Cheese", "Emmental", "Fontina", "Gouda", "Manchego", "Paneer", "Queso Fresco", "Cotija", "Burrata", "Humboldt Fog",
        "Stinking Bishop", "Comté", "Cantal", "Cheshire", "Lancashire", "Double Gloucester", "Neufchâtel", "Reblochon", "Taleggio", "Bechamel Cheese",
        "Fior di Latte", "Ricotta Salata", "Pecorino Romano", "Grana Padano", "Grana", "Fontal", "Piave", "Scamorza", "Stracchino", "Toma",
        "Valdeon", "Spenwood", "Cotswold", "Danish Blue", "Roquefort", "Bastardo", "Tomme", "Livarot", "Mimolette", "Pecorino Sardo"
    ]
}

# Generate dataset

num_samples_per_category = 1000
ingredients = []
labels = []

for category, items in categories.items():
    for _ in range(num_samples_per_category):
        ingredient = random.choice(items)
        ingredients.append(ingredient.lower())
        labels.append(category.lower())

# Shuffle the dataset
dataset = list(zip(ingredients, labels))
random.shuffle(dataset)
ingredients, labels = zip(*dataset)

# Create DataFrame
df = pd.DataFrame({
    "Ingredient": ingredients,
    "Category": labels
})

# Split the dataset into train and test
train_df, test_df = train_test_split(df, test_size=0.2, random_state=42)

# Save to CSV
train_df.to_csv("train.csv", index=False)
test_df.to_csv("test.csv", index=False)

print("Dataset split and saved as 'train.csv' and 'test.csv'.")