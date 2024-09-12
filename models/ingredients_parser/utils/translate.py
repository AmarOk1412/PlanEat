from googletrans import Translator
import json

translator = Translator()
i = 0

with open('final.json') as json_data:
    d = json.loads(json_data.read())
    final = []
    for row in range(0, len(d)):
        print(f"Translating row : {row}/{len(d)}")
        try:
            context = d[row].get("Context")
            context_sp = translator.translate(context, src='fr', dest='es').text
            context_en = translator.translate(context, src='fr', dest='en').text
            context_ge = translator.translate(context, src='fr', dest='de').text
            answers = d[row].get("Answer")
            answers = answers.replace("'",'"')
            answers_json = json.loads(answers)
            d[row]["Answer"] = json.dumps(answers_json)
            final.append(d[row])
            ingredients_sp = []
            ingredients_en = []
            ingredients_ge = []
            for answer in answers_json:
                ingredient = answer.get("ingredient")
                ingredient_sp = answer
                ingredient_en = answer
                ingredient_ge = answer
                ingredient_sp["ingredient"] = translator.translate(ingredient, src='fr', dest='es').text
                ingredient_en["ingredient"] = translator.translate(ingredient, src='fr', dest='en').text
                ingredient_ge["ingredient"] = translator.translate(ingredient, src='fr', dest='de').text
                ingredients_sp.append(ingredient_sp)
                ingredients_en.append(ingredient_en)
                ingredients_ge.append(ingredient_ge)
            final.append({
                "Context": context_sp,
                "Question": "parse",
                "Answer": json.dumps(ingredients_sp)
                })
            final.append({
                "Context": context_en,
                "Question": "parse",
                "Answer": json.dumps(ingredients_en)
                })
            final.append({
                "Context": context_ge,
                "Question": "parse",
                "Answer": json.dumps(ingredients_ge)
                })
        except:
            final.append(d[row])
            print(d[row])
            continue

with open('final2.json', 'w') as f:
    f.write(json.dumps(final))
