#!/usr/bin/env python

import random

def write_to_file(line, filename, mode='a'):
    with open(filename, mode, newline='') as file:
        file.write(line)

def main():
    write_to_file('sentence@label\n', 'train.csv', 'w')
    write_to_file('sentence@label\n', 'test.csv', 'w')

    with open('output.csv', 'r') as file:
        for line in file:
            probability = random.random()
            if probability <= 0.7:
                write_to_file(line, 'train.csv')
            else:
                write_to_file(line, 'test.csv')

if __name__ == '__main__':
    main()