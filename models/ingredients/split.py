#!/usr/bin/env python

import csv
import random

def write_to_file(line, filename):
    with open(filename, 'a', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(line)

def main():
    with open('output.csv', 'r') as file:
        reader = csv.reader(file)
        for line in reader:
            if line:
                probability = random.random()
                if probability <= 0.7:
                    write_to_file(line, 'train.csv')
                else:
                    write_to_file(line, 'test.csv')

if __name__ == '__main__':
    main()