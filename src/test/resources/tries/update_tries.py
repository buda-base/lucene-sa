import sys
import os


def parse_total_lexicon():
    with open('../../../../resources/sanskrit-stemming-data/output/trie_content.txt') as f:
        content = f.read().split('\n')
    
    total_output = {}
    for c in content:
        form = c.split(',')[0]
        total_output[form] = c
    return total_output


def update_tries(lexicon):
    for file in os.listdir('.'):
        with open(file, 'r') as f:
            content = f.read().split('\n')
        
        new_content = []
        for line in content:
            form = line.split(',')[0]
            if form in lexicon.keys():
                new_content.append(lexicon[form])
            else:
                new_content.append(line)
        
        with open(file, 'w', 1, 'utf-8') as g:
            g.write('\n'.join(new_content))


if __name__ == '__main__':
    total_lexicon = parse_total_lexicon()
    update_tries(total_lexicon)
