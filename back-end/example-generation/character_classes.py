from typing import Iterable

# just the type of a single character
# helpful for distinguishing between single characters and long strings
Character = str

class CharacterClass:
    '''Contains valid characters for a given class. Makes it easy to add and subtract and iterate through character classes.'''
    def __init__(self, characters: Iterable[Character]):
        self.characters: Set[Character] = set(characters)
        # just for the assertion
        for character in self.characters:
            assert len(character) == 1, f"Character with representation {repr(character)} did not have length 1"

    def __add__(self, other) -> 'CharacterClass':
        # union the two sets
        # from https://stackoverflow.com/a/29648563
        return CharacterClass(self.characters | other.characters)

    def __radd__(self, other):
        '''Needed for summing. (See https://stackoverflow.com/a/60513363)'''
        if other == 0:
            return self
        else:
            return self.__add__(other)

    def __sub__(self, other):
        return CharacterClass(self.characters - other.characters)

    def __iter__(self):
        return iter(self.characters)

    def __str__(self) -> str:
        return f"<CharacterClass: {self.characters}>"

LOWERCASE = CharacterClass('abcdefghijklmnopqrstuvwxyz')
UPPERCASE = CharacterClass('ABCDEFGHIJKLMNOPQRSTUVWXYZ')
LETTER: 'CharacterClass' = LOWERCASE + UPPERCASE
DIGIT = CharacterClass('1234567890')
# TODO find out what characters are space
SPACE = CharacterClass(' ')
# TODO find out what characters are special
SPECIAL = CharacterClass('-=!@#$%^&*()_+\\\'"')
ALL: 'CharacterClass' = LETTER + DIGIT + SPACE + SPECIAL
