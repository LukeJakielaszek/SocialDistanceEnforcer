import os
import sys

#matplotlib inline
#config InlineBackend.figure_format = 'retina'
import matplotlib.pyplot as plt

# create a map holding data for each device distance measurements
def map_files_device(bfiles, direc):

    # our device map
    bmaps = {}

    # loop through each file in our dataset
    for bfile in bluetooth_files:
        temp = bfile

        # parse the filename for device info
        temp = temp.split('_')
        device = temp[0]
        class_a = temp[2]
        major = temp[4]
        dist = temp[5]

        # if its our first time seeing the device, add it to our map
        if not device in bmaps:
            dev_map = {}
            dev_map["device"] = device
            dev_map["class"] = class_a
            dev_map["major"] = major
            dev_map["dist"] = {}
            dev_map["files"] = []

            bmaps[device] = dev_map
            

        # get the sensor data from the file
        data = get_data(direc + bfile)

        # record the data captured at an associated distance
        bmaps[device]["dist"][dist] = data

        # record the file name
        bmaps[device]["files"].append(bfile)

    # return our device mapping
    return bmaps

# parse sensor data from a file
def get_data(bfile):
    # returnable data
    data = []

    # open the file
    with open(bfile, "r") as bf:
        # get each line
        lines = bf.readlines()

        # extract data from each line
        for line in lines:
            line = line.rstrip()
            line = line.split('|')[1]
            line= int(line)
            data.append(line)

    # return our list of data
    return data

# converts a device map into a list of (X,y) coords for each device
def get_coords_device(bmaps):

    # coordinate mapping
    coords = {}

    # loop through each device
    for device in bmaps:
        coords[device] = []
        dists = bmaps[device]['dist']

        # extract one sample of data as a coordinate where x is distance, y is RSSI value
        for dist in dists:
            for val in dists[dist]:
                coords[device].append((int(dist), int(val)))

    for device in coords:
        print("NUM_ITEMS_DEVICE ", len(coords[device]))
        
    # return our extracted coord lists
    return coords

# converts a class map into a list of (X,y) coords for each class
def get_coords_class(bmaps):
    # coordinate mapping
    coords = {}

    # loop through each device
    for device, dists in zip(bmaps.keys(), bmaps.values()):
        # construct our class key
        coord_key = dists["class"] + "_" + dists["major"]

        # if we have not seen a key, initialize one
        if not coord_key in coords:
            coords[coord_key] = []

        dists = dists["dist"]
        # extract one sample of data as a coordinate where x is distance, y is RSSI value
        for dist in dists:
            for val in dists[dist]:
                coords[coord_key].append((int(dist), int(val)))

    for device in coords:
        print("NUM_ITEMS_CLASS " , len(coords[device]))
                
    # return our extracted coord lists
    return coords

# plot the data and prep data for KNN implementations
def plot_data(coords):
    # aggregate data
    sum_data = {}

    # instantiate our figure
    plt.figure(figsize=(25,15))
    plt.title('Bluetooth Distances', fontsize=25, fontweight='bold')

    # loop over each device
    for device in coords:        
        device_coords = coords[device]

        print(device)
        print("\tTotal Coords [" + str(len(device_coords)) + "]")

        # extract x and y vectors for all coords
        x,y = zip(*device_coords)

        print("\t\tTotal X [" + str(len(x)) + "]")
        print("\t\tTotal Y [" + str(len(y)) + "]")


        # create a dictionary aggregating overlapping coordinates and tracking the count for each
        # overlapping coord
        size_dict = {}
        for xs, ys in zip(x,y):
            if not xs in size_dict:
                size_dict[xs] = {}

            if(not ys in size_dict[xs]):
                size_dict[xs][ys] = 0

            size_dict[xs][ys] += 1

        # get counts for each non-overlapping coordinate
        xc = []
        yc = []
        size_list = []
        for size, dict_a in zip(size_dict.keys(), size_dict.values()):
            for dist, count in zip(dict_a.keys(), dict_a.values()):
                xc.append(size)
                yc.append(dist)
                size_list.append(count)

        # store our aggregated data        
        sum_data[device] = {}
        sum_data[device]['x'] = xc
        sum_data[device]['y'] = yc
        sum_data[device]['count'] = size_list.copy()

        for i,val in enumerate(size_list):
            size_list[i] = val*100
                
        # plot each non-overlapping coordinate with an associated count representing size of dot
        plt.scatter(xc,yc, alpha=.3, s=size_list, label=device)

    # generate our plot
    plt.legend(title="Devices", loc="upper right", prop={'size': 25})
    plt.xlabel('Dist (ft)', fontsize=25, fontweight='bold')
    plt.ylabel('RSSI', fontsize=25, fontweight='bold')
    plt.savefig("Bluetooth_RSSI_distances")
    plt.show()

    # return our aggregate data
    return sum_data

# format and save our info to files
def format_output(sum_data, coords):
    print(sum_data)
    for major, dicts in zip(sum_data.keys(), sum_data.values()):
        print(major)

        ofile_name = "proc_data/" + major + ".txt"
        print('\tOutput File :' + ofile_name)
        with open(ofile_name, "a") as ofile:
            xdata = dicts["x"]
            ydata = dicts["y"]
            count_data = dicts["count"]

            for x,y,z in zip(xdata,ydata,count_data):
                ofile.write(str(x) + "," + str(y) + "," + str(z) + "\n")
    
if __name__ == "__main__":
    direc = sys.argv[1]
    bluetooth_files = os.listdir(direc)

    bmaps = map_files_device(bluetooth_files, direc)    

    print(bmaps)
    for device in bmaps:
        print(device)
        sum_samples = 0
        for dist in bmaps[device]['dist']:
            print("\tDist [" + str(dist) + "] + LEN [" +
                  str(len(bmaps[device]['dist'][dist])) + "]")

            sum_samples += len(bmaps[device]['dist'][dist])

        print("\t\tTotal Samples [" + str(sum_samples) + "]")

    coords_device = get_coords_device(bmaps)
    coords_class = get_coords_class(bmaps)

    # plot data by device
    sum_data = plot_data(coords_device)

    # plot data by class
    sum_data_class = plot_data(coords_class)

    # save our info to files
    format_output(sum_data_class, coords_class)
