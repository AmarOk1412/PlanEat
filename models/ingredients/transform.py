import json
import random

cnt = 0

def parse_data(data):
    # Split the data into components
    components = [line.split("|") for line in data]
    quantity_str = ""
    quantity_pos = 0
    quantity = []###""
    unit_str = ""
    unit_pos = 0
    unit = []###""
    ingredient_str = ""
    ingredient_pos = 0
    buffer_str = ""
    ingredient = []###""
    last_label = 'None'
    context = ''
    label = ''
    # For each component, create a question and answer
    for component in components:
        context, label = component[1].split("@")
        context = context.strip()
        label = label.strip()

        # Get the label, text, and position
        text = component[0].strip()
        position = context.find(text)
        if label == 'QUANTITY':
            quantity_str += text + " "
            if last_label != 'QUANTITY':
                quantity_pos = position
        elif label == 'UNIT':
            unit_str += text + " "
            if last_label != 'UNIT':
                unit_pos = position
        elif label == 'INGREDIENT':
            if buffer_str != "":
                ingredient_str += buffer_str
                buffer_str = ""
            ingredient_str += text + " "
            if last_label != 'INGREDIENT':
                ingredient_pos = position
        elif label == 'AND' or label == 'OR':
            buffer_str = ''
        elif label == 'NONE' and ingredient_str != "":
            buffer_str += text
            if len(text)>0 and text[-1] != "'":
                buffer_str += " "

        is_end = components.index(component) == len(components) - 1 or label == 'AND' or label == 'OR'
        if label != last_label or is_end:
            if last_label == 'QUANTITY' and quantity_str != "":
                quantity.append({
                    "text": quantity_str.strip(),
                    "answer_start": quantity_pos
                })
                ####quantity += quantity_str.strip()
                quantity_str = ""
                buffer_str = ""
            if last_label == 'UNIT' and unit_str != "":
                unit.append({
                    "text": unit_str.strip(),
                    "answer_start": unit_pos
                })
                ###unit += unit_str.strip()
                unit_str = ""
                buffer_str = ""
            if ingredient_str != "" and ((label != 'NONE' and last_label == 'INGREDIENT') or is_end):
                ingredient.append({
                    "text": ingredient_str.strip(),
                    "answer_start": ingredient_pos
                })
                ###ingredient += ingredient_str.strip()
                ingredient_str = ""
            if label != 'NONE':
                last_label = label

    if quantity == "":
        quantity = "None"
    if unit == "":
        unit = "None"
    ###output = f'context is {context} , label is quantity , answers = {quantity}\n'
    ###output += f'context is {context} , label is unit , answers = {unit}\n'
    ###output += f'context is {context} , label is ingredient , answers = {ingredient}\n'
###
    ###return output

    # Create a question and answer
    qas = []
    global cnt
    cnt += 1
    id_str = str(cnt)
    qas.append({
        "id": id_str,
        "question": "quantity?",
        "is_impossible": len(quantity) == 0,
        "answers": quantity
    })
    cnt += 1
    id_str = str(cnt)
    qas.append({
        "id": id_str,
        "question": "unit?",
        "is_impossible": len(unit) == 0,
        "answers": unit
    })
    cnt += 1
    id_str = str(cnt)
    qas.append({
        "id": id_str,
        "question": "ingredient?",
        "is_impossible": len(ingredient) == 0,
        "answers": ingredient
    })
    # Create the final output
    output = {
        ##"paragraphs": [
            ##{
                "context": context,
                "qas": qas
            }
        ##]
    ##}

    return json.dumps(output)

def read_file():
    with open('final.json', 'w') as outf:
        with open('final_val.json', 'w') as outf_v:
            ##outf.write('{"version":"v2.0","data":[\n')
            ##outf_v.write('{"version":"v2.0","data":[\n')
            outf.write('[\n')
            outf_v.write('[\n')
            data = []
            with open('input.csv') as f:
                last_context = ''
                for line in f:
                    components = line.split("|")
                    context, label = components[1].split("@")
                    if last_context == '':
                        last_context = context.strip()
                    if last_context != context.strip():
                        if random.random() < 0.3:
                            outf_v.write(parse_data(data))
                            outf_v.write(',\n')
                        else:
                            outf.write(parse_data(data))
                            outf.write(',\n')
                        data = []
                        last_context = context.strip()
                    data.append(line.strip())
            outf.write(parse_data(data)) # Parse the last data
            outf_v.write(parse_data(data)) # Parse the last data
            ##outf.write('\n]}')
            ##outf_v.write('\n]}')
            outf.write('\n]')
            outf_v.write('\n]')



if __name__ == '__main__':
    read_file()