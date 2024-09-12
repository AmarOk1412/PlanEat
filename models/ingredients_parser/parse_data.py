#!/usr/bin/env python3
import csv

def parse_line(line):
    word, words, optional1, optional2, word2 = [field.strip() for field in line]

    if word == optional1:
        return f"{word} | {words} | QUANTITY"
    elif word == optional2:
        return f"{word} | {words} | UNIT"
    elif word in words.split():
        return f"{word} | {words} | INGREDIENT"
    else:
        return f"{word} | {words} | UNKNOWN"

with open('data.csv', 'r') as file:
    reader = csv.reader(file, delimiter='|')
    for row in reader:
        print(parse_line(row))