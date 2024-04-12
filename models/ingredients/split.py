#!/usr/bin/env python

import csv
import random

def write_to_file(line, filename, mode='a'):
    with open(filename, 'a', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(line)

def main():
    write_to_file('sentence @ label', 'train.csv', 'w')
    write_to_file('sentence @ label', 'test.csv', 'w')

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