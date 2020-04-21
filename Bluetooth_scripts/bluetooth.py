import os
import sys

#matplotlib inline
#config InlineBackend.figure_format = 'retina'
import matplotlib.pyplot as plt

def map_files(bfiles, direc):
    bmaps = {}
    for bfile in bluetooth_files:
        temp = bfile

        temp = temp.split('_')

        device = temp[0]
        class_a = temp[2]
        major = temp[4]
        dist = temp[5]
        
        if not device in bmaps:
            dev_map = {}
            dev_map["device"] = device
            dev_map["class"] = class_a
            dev_map["major"] = major
            dev_map["dist"] = {}
            dev_map["files"] = []

            bmaps[device] = dev_map
            

        data = get_data(direc + bfile)

        bmaps[device]["dist"][dist] = data
        
        bmaps[device]["files"].append(bfile)

    return bmaps

def get_data(bfile):
    data = []
    with open(bfile, "r") as bf:
        lines = bf.readlines()

        for line in lines:
            line = line.rstrip()
            line = line.split('|')[1]
            line= int(line)
            data.append(line)

    return data

def get_coords(bmaps):
    coords = {}
    for device in bmaps:
        coords[device] = []
        dists = bmaps[device]['dist']
        
        for dist in dists:
            for val in dists[dist]:
                coords[device].append((int(dist), float(val)))
    return coords

def plot_data(coords):
    plt.figure(figsize=(25,15))
    plt.title('Bluetooth Distances', fontsize=25, fontweight='bold')
    
    for device in coords:
        device_coords = coords[device]

        print(device)
        print("\tTotal Coords [" + str(len(device_coords)) + "]")

        #x, y = zip(*device_coords)
        x,y = zip(*device_coords)
        
        print("\t\tTotal X [" + str(len(x)) + "]")
        print("\t\tTotal Y [" + str(len(y)) + "]")


        size_dict = {}
        for xs, ys in zip(x,y):
            if not xs in size_dict:
                size_dict[xs] = {}

            if(not ys in size_dict[xs]):
                size_dict[xs][ys] = 0

            size_dict[xs][ys] += 1

        '''
        size_list = []
        for xs, ys in zip(x,y):
            size_list.append(size_dict[xs][ys] * 20)
        '''
        
        xc = []
        yc = []
        size_list = []
        for size, dict_a in zip(size_dict.keys(), size_dict.values()):
            for dist, count in zip(dict_a.keys(), dict_a.values()):
                xc.append(size)
                yc.append(dist)
                size_list.append(count*100)

        plt.scatter(xc,yc, alpha=.25, s=size_list, label=device)
        '''
        for dist in dists:
            data = dists[dist]
            print(data)
            plt.plot(data, 's', label=device)
        '''
    plt.legend(title="Devices", loc="upper right", prop={'size': 25})
    plt.xlabel('Dist (ft)', fontsize=25, fontweight='bold')
    plt.ylabel('RSSI', fontsize=25, fontweight='bold')
    #    plt.savefig("Bluetooth_RSSI_distances.jpg")
    plt.show()

if __name__ == "__main__":
    direc = sys.argv[1]
    bluetooth_files = os.listdir(direc)

    bmaps = map_files(bluetooth_files, direc)    

    for device in bmaps:
        print(device)
        sum_samples = 0
        for dist in bmaps[device]['dist']:
            print("\tDist [" + str(dist) + "] + LEN [" +
                  str(len(bmaps[device]['dist'][dist])) + "]")

            sum_samples += len(bmaps[device]['dist'][dist])

        print("\t\tTotal Samples [" + str(sum_samples) + "]")

    coords = get_coords(bmaps)

    plot_data(coords)
