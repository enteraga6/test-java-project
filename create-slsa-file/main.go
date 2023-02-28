package main

import (
	"encoding/json"
	"crypto/sha256"
	"fmt"
	"strings"
	"os"
)

type OutputSlsaFile struct {
	Version int 				`json:"version"`
	Attestations []*Attestation `json:"attestations"`
}

type Attestation struct {
	Name string					`json:"name"`
	Subjects []*Subject			`json:"subjects"`
}

type Subject struct {
	Name string					`json:"name"`
	Digest map[string]string	`json:"digest"`
}
func NewSHA256(data []byte) []byte {
	hash := sha256.Sum256(data)
	return hash[:]
}

func main() {
	args := os.Args[1:]
	if len(args) == 0 {
		panic("Need at least one artifact file")
	}
	slsaFile := &OutputSlsaFile{
		Version: 1,
		Attestations: make([]*Attestation, 0),
	}
	for _, artifact := range args {
		artifactFileName := strings.Split(artifact, "/target/")[1]
		digest := NewSHA256([]byte(artifactFileName))
		fmt.Println(artifactFileName)
		fmt.Printf("%x\n", digest[:])
		attestation := &Attestation{
			Name: "attestation1.intoto",
			Subjects: []*Subject{
				&Subject {
					Name: artifactFileName,
					Digest: map[string]string{
						"sha256": fmt.Sprintf("%x", digest),
					},
				},
			},
		}

		slsaFile.Attestations = append(slsaFile.Attestations, attestation)
	}
	out, err := json.MarshalIndent(slsaFile, "", "  ")
	if err != nil {
		panic(err)
	}
	fmt.Println(string(out))
}