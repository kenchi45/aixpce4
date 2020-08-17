package com.example.androidtest

class Debug {
    fun TRACE(str: String?) {}
    fun TRACE(str: String?, a: Int) {}
    fun TRACE(str: String?, a: Int, b: Int) {}
    fun TRACE(str: String?, a: Int, b: Int, c: Int) {}

    companion object {
        private const val BUF_N = 16
        private val buf_I =
            IntArray(BUF_N)
        private val buf_PC =
            IntArray(BUF_N)
        private val buf_A =
            IntArray(BUF_N)
        private val buf_P =
            IntArray(BUF_N)
        private val buf_X =
            IntArray(BUF_N)
        private val buf_Y =
            IntArray(BUF_N)
        private val buf_S =
            IntArray(BUF_N)
        private val buf_USER =
            IntArray(BUF_N)
        private var cycleCount = 0
        fun addDebugMessage(
            I: Int,
            PC: Int,
            A: Int,
            P: Int,
            X: Int,
            Y: Int,
            S: Int,
            USER: Int
        ) {
            buf_I[cycleCount] =
                I
            buf_PC[cycleCount] =
                PC
            buf_A[cycleCount] =
                A
            buf_P[cycleCount] =
                P
            buf_X[cycleCount] =
                X
            buf_Y[cycleCount] =
                Y
            buf_S[cycleCount] =
                S
            buf_USER[cycleCount] =
                USER
            cycleCount =
                cycleCount + 1 and BUF_N - 1
        }

        fun printDebugMessage() {
            for (i in 0 until BUF_N) {
                println(
                    "I=0x" + Integer.toHexString(
                        buf_I[cycleCount]
                    )
                            + " PC=" + buf_PC[cycleCount]
                            + " A=" + buf_A[cycleCount]
                            + " P=" + buf_P[cycleCount]
                            + " X=" + buf_X[cycleCount]
                            + " Y=" + buf_Y[cycleCount]
                            + " S=" + buf_S[cycleCount]
                )
                cycleCount =
                    (cycleCount + 1) % BUF_N
            }
        }

        fun getOpcodeString(opcode: Int): String {
            var opcodeStr = ""
            opcodeStr = when (opcode) {
                0x00 -> "BRK"
                0x01 -> "ORA"
                0x02 -> "SXY"
                0x03 -> "ST0"
                0x04 -> "TSB"
                0x05 -> "ORA"
                0x06 -> "ASL"
                0x07 -> "RMB0"
                0x08 -> "PHP"
                0x09 -> "ORA"
                0x0A -> "ASL"
                0x0B -> "undefined"
                0x0C -> "TSB"
                0x0D -> "ORA"
                0x0E -> "ASL"
                0x0F -> "BBR0"
                0x10 -> "BPL"
                0x11 -> "ORA"
                0x12 -> "ORA"
                0x13 -> "ST1"
                0x14 -> "TRB"
                0x15 -> "ORA"
                0x16 -> "ASL"
                0x17 -> "RMB1"
                0x18 -> "CLC"
                0x19 -> "ORA"
                0x1A -> "INC"
                0x1B -> "undefined"
                0x1C -> "TRB"
                0x1D -> "ORA"
                0x1E -> "ASL"
                0x1F -> "BBR1"
                0x20 -> "JSR"
                0x21 -> "AND"
                0x22 -> "SAX"
                0x23 -> "ST2"
                0x24 -> "BIT"
                0x25 -> "AND"
                0x26 -> "ROL"
                0x27 -> "RMB2"
                0x28 -> "PLP"
                0x29 -> "AND"
                0x2A -> "ROL"
                0x2B -> "undefined"
                0x2C -> "BIT"
                0x2D -> "AND"
                0x2E -> "ROL"
                0x2F -> "BBR2"
                0x30 -> "BMI"
                0x31 -> "AND"
                0x32 -> "AND"
                0x33 -> "undefined"
                0x34 -> "BIT"
                0x35 -> "AND"
                0x36 -> "ROL"
                0x37 -> "RMB3"
                0x38 -> "SEC"
                0x39 -> "AND"
                0x3A -> "DEC"
                0x3B -> "undefined"
                0x3C -> "BIT"
                0x3D -> "AND"
                0x3E -> "ROL"
                0x3F -> "BBR3"
                0x40 -> "RTI"
                0x41 -> "EOR"
                0x42 -> "SAY"
                0x43 -> "TMA"
                0x44 -> "BSR"
                0x45 -> "EOR"
                0x46 -> "LSR"
                0x47 -> "RMB4"
                0x48 -> "PHA"
                0x49 -> "EOR"
                0x4A -> "LSR"
                0x4B -> "undefined"
                0x4C -> "JMP"
                0x4D -> "EOR"
                0x4E -> "LSR"
                0x4F -> "BBR4"
                0x50 -> "BVC"
                0x51 -> "EOR"
                0x52 -> "EOR"
                0x53 -> "TAM"
                0x54 -> "CSL"
                0x55 -> "EOR"
                0x56 -> "LSR"
                0x57 -> "RMB5"
                0x58 -> "CLI"
                0x59 -> "EOR"
                0x5A -> "PHY"
                0x5B -> "undefined"
                0x5C -> "undefined"
                0x5D -> "EOR"
                0x5E -> "LSR"
                0x5F -> "BBR5"
                0x60 -> "RTS"
                0x61 -> "ADC"
                0x62 -> "CLA"
                0x63 -> "undefined"
                0x64 -> "STZ"
                0x65 -> "ADC"
                0x66 -> "ROR"
                0x67 -> "RMB6"
                0x68 -> "PLA"
                0x69 -> "ADC"
                0x6A -> "ROR"
                0x6B -> "undefined"
                0x6C -> "JMP"
                0x6D -> "ADC"
                0x6E -> "ROR"
                0x6F -> "BBR6"
                0x70 -> "BVS"
                0x71 -> "ADC"
                0x72 -> "ADC"
                0x73 -> "TII"
                0x74 -> "STZ"
                0x75 -> "ADC"
                0x76 -> "ROR"
                0x77 -> "RMB7"
                0x78 -> "SEI"
                0x79 -> "ADC"
                0x7A -> "PLY"
                0x7B -> "undefined"
                0x7C -> "JMP"
                0x7D -> "ADC"
                0x7E -> "ROR"
                0x7F -> "BBR7"
                0x80 -> "BRA"
                0x81 -> "STA"
                0x82 -> "CLX"
                0x83 -> "TST"
                0x84 -> "STY"
                0x85 -> "STA"
                0x86 -> "STX"
                0x87 -> "SMB0"
                0x88 -> "DEY"
                0x89 -> "BIT"
                0x8A -> "TXA"
                0x8B -> "undefined"
                0x8C -> "STY"
                0x8D -> "STA"
                0x8E -> "STX"
                0x8F -> "BBS0"
                0x90 -> "BCC"
                0x91 -> "STA"
                0x92 -> "STA"
                0x93 -> "TST"
                0x94 -> "STY"
                0x95 -> "STA"
                0x96 -> "STX"
                0x97 -> "SMB1"
                0x98 -> "TYA"
                0x99 -> "STA"
                0x9A -> "TXS"
                0x9B -> "undefined"
                0x9C -> "STZ"
                0x9D -> "STA"
                0x9E -> "STZ"
                0x9F -> "BBS1"
                0xA0 -> "LDY"
                0xA1 -> "LDA"
                0xA2 -> "LDX"
                0xA3 -> "TST"
                0xA4 -> "LDY"
                0xA5 -> "LDA"
                0xA6 -> "LDX"
                0xA7 -> "SMB2"
                0xA8 -> "TAY"
                0xA9 -> "LDA"
                0xAA -> "TAX"
                0xAB -> "undefined"
                0xAC -> "LDY"
                0xAD -> "LDA"
                0xAE -> "LDX"
                0xAF -> "BBS2"
                0xB0 -> "BCS"
                0xB1 -> "LDA"
                0xB2 -> "LDA"
                0xB3 -> "TST"
                0xB4 -> "LDY"
                0xB5 -> "LDA"
                0xB6 -> "LDX"
                0xB7 -> "SMB3"
                0xB8 -> "CLV"
                0xB9 -> "LDA"
                0xBA -> "TSX"
                0xBB -> "undefined"
                0xBC -> "LDY"
                0xBD -> "LDA"
                0xBE -> "LDX"
                0xBF -> "BBS3"
                0xC0 -> "CPY"
                0xC1 -> "CMP"
                0xC2 -> "CLY"
                0xC3 -> "TDD"
                0xC4 -> "CPY"
                0xC5 -> "CMP"
                0xC6 -> "DEC"
                0xC7 -> "SMB4"
                0xC8 -> "INY"
                0xC9 -> "CMP"
                0xCA -> "DEX"
                0xCB -> "undefined"
                0xCC -> "CPY"
                0xCD -> "CMP"
                0xCE -> "DEC"
                0xCF -> "BBS4"
                0xD0 -> "BNE"
                0xD1 -> "CMP"
                0xD2 -> "CMP"
                0xD3 -> "TIN"
                0xD4 -> "CSH"
                0xD5 -> "CMP"
                0xD6 -> "DEC"
                0xD7 -> "SMB5"
                0xD8 -> "CLD"
                0xD9 -> "CMP"
                0xDA -> "PHX"
                0xDB -> "undefined"
                0xDC -> "undefined"
                0xDD -> "CMP"
                0xDE -> "DEC"
                0xDF -> "BBS5"
                0xE0 -> "CPX"
                0xE1 -> "SBC"
                0xE2 -> "undefined"
                0xE3 -> "TIA"
                0xE4 -> "CPX"
                0xE5 -> "SBC"
                0xE6 -> "INC"
                0xE7 -> "SMB6"
                0xE8 -> "INX"
                0xE9 -> "SBC"
                0xEA -> "NOP"
                0xEB -> "undefined"
                0xEC -> "CPX"
                0xED -> "SBC"
                0xEE -> "INC"
                0xEF -> "BBS6"
                0xF0 -> "BEQ"
                0xF1 -> "SBC"
                0xF2 -> "SBC"
                0xF3 -> "TAI"
                0xF4 -> "SET"
                0xF5 -> "SBC"
                0xF6 -> "INC"
                0xF7 -> "SMB7"
                0xF8 -> "SED"
                0xF9 -> "SBC"
                0xFA -> "PLX"
                0xFB -> "undefined"
                0xFC -> "[BLE]"
                0xFD -> "SBC"
                0xFE -> "INC"
                0xFF -> "BBS7"
                else -> ""
            }
            return opcodeStr
        }
    }
}